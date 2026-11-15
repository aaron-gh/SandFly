package com.google.android.accessibility.talkback.focusmanagement;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TargetType;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Table navigation for the Google Sheets grid.
 *
 * <p>Sheets labels each cell with its address ("B13", or "A2; 162 Has formula ...") but sets no
 * CollectionInfo or CollectionItemInfo, so the grid isn't a table to the usual table code. It also
 * lists only the cells before and after the focused one in reading order (row by row), so a cell
 * in another row can only be reached by stepping through the cells in between. This class does
 * that stepping without speaking the cells passed over, and gives the target cell back for the
 * caller to focus and speak.
 *
 * <p>Each step waits for Sheets to list the cells around the newly focused one. The waiting is
 * posted to the main thread, not slept, so a move that needs it ends later through {@link Later}.
 * A touch on the screen or a focus move by the user stops the move in progress.
 */
public final class SheetsGrid {

  private static final String TAG = "SheetsGrid";
  public static final String PACKAGE = "com.google.android.apps.docs.editors.sheets";

  /** A cell's label starts with its address, then ends or goes on after a semicolon or space. */
  private static final Pattern CELL = Pattern.compile("^([A-Z]{1,3})([1-9][0-9]{0,6})(?:$|[;,\\s])");

  /** At most this many cells are passed over in one move. */
  private static final int MAX_STEPS = 400;

  /** How long to wait for Sheets to list the cells around a newly focused cell. */
  private static final long SETTLE_TIMEOUT_MS = 300;

  private static final long POLL_MS = 8;

  /** The longest a move runs on the main thread at a time before it lets other work run. */
  private static final long MAX_BLOCK_MS = 30;

  /** After this long, a move stops stepping through cells and takes the best cell found so far. */
  private static final long MAX_STEPPING_MS = 10_000;

  /** A move still running after this long (the service stopped, say) is dropped. */
  private static final long MAX_MOVE_MS = 20_000;

  /** A column move scrolls the grid sideways at most this many times looking for the next cell. */
  private static final int MAX_SCROLLS = 3;

  /** How long the drag that scrolls the grid takes, and how long the finger then rests. */
  private static final long DRAG_MS = 250;

  private static final long REST_MS = 150;

  /** How long to wait for the cells to move after a drag. */
  private static final long SCROLL_TIMEOUT_MS = 300;

  /** How long to wait for Sheets to list the cells around the focused one after a scroll. */
  private static final long RELIST_TIMEOUT_MS = 1000;

  /** How long to wait for Sheets to list the cells again after one focus change. */
  private static final long RELIST_STEP_MS = 400;

  /** How long Android takes to apply the pass-through region the drag goes through. */
  private static final long PASS_THROUGH_WAIT_MS = 350;

  /** Half the height of the pass-through strip around the drag. */
  private static final int PASS_THROUGH_MARGIN = 80;

  /** Where each drag starts, in rows (or columns) from the cell towards the grid's middle. */
  private static final int[] DRAG_OFFSETS = {1, -1, 2};

  /** How far a sideways drag stays from the grid's edges. */
  private static final int EDGE_MARGIN = 60;

  /** How often, at most, the window is searched for a grid that lists no cells. */
  private static final long GRID_SEARCH_INTERVAL_MS = 200;

  private static final Handler handler = new Handler(Looper.getMainLooper());

  /** The move in progress, or null. Used on the main thread only. */
  @Nullable private static Move active = null;

  /**
   * Whether an injected touch rests on the grid so Sheets lists its cells again: its hover events
   * must reach SandFly, which focuses the cell under it, so they aren't dropped.
   */
  private static boolean touching = false;

  /**
   * Whether a gesture SandFly injected is on the screen, so the touch interaction it starts isn't
   * taken for the user's.
   */
  private static boolean injecting = false;

  /** Lets touches through to the app in a region of the screen, or stops that when null. */
  public interface PassThrough {
    void set(@Nullable Region region);
  }

  /** Gets the cell a move found after waiting for Sheets, or null when there was none. */
  public interface Later {
    void found(@Nullable AccessibilityNodeInfoCompat cell);
  }

  /**
   * Focus events from cells passed over are dropped until this time. Written and read on the
   * main thread, volatile in case events are ever processed elsewhere.
   */
  private static volatile long silentUntil = 0;

  /** The address to speak at the end of a move, the only cell not dropped while silent. */
  @Nullable private static volatile String silentTarget = null;

  /** A cell whose next focus event is dropped until {@link #quietUntil}: a stopped move's pivot. */
  @Nullable private static String quietAddress = null;

  private static long quietUntil = 0;

  private SheetsGrid() {}

  /** A cell address, counted from 0. */
  public static final class Cell {
    public final int row;
    public final int column;
    public final String address;

    Cell(int row, int column, String address) {
      this.row = row;
      this.column = column;
      this.address = address;
    }

    /** Compares by reading order: row by row, then column by column. */
    int compareTo(Cell other) {
      return row != other.row ? Integer.compare(row, other.row) : Integer.compare(column, other.column);
    }

    @Override
    public String toString() {
      return address;
    }
  }

  /** Returns the cell address of a node in the Sheets grid, or null. */
  @Nullable
  public static Cell cellOf(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null || !TextUtils.equals(node.getPackageName(), PACKAGE)) {
      return null;
    }
    return parse(node.getContentDescription());
  }

  @Nullable
  static Cell parse(@Nullable CharSequence description) {
    if (description == null) {
      return null;
    }
    Matcher m = CELL.matcher(description);
    if (!m.find()) {
      return null;
    }
    String letters = m.group(1);
    int column = 0;
    for (int i = 0; i < letters.length(); i++) {
      column = column * 26 + (letters.charAt(i) - 'A' + 1);
    }
    int row;
    try {
      row = Integer.parseInt(m.group(2));
    } catch (NumberFormatException e) {
      return null;
    }
    return new Cell(row - 1, column - 1, letters + m.group(2));
  }

  /** The grid's range label names two cells: "Grid range: from B1 to D21 ...". */
  private static final Pattern RANGE =
      Pattern.compile("\\b[A-Z]{1,3}[1-9][0-9]*\\b.*\\b[A-Z]{1,3}[1-9][0-9]*\\b");

  /**
   * When the focus is on the Sheets grid's range label and the grid lists no cells (after a
   * scroll a swipe interrupted), rests a touch on the middle of the grid so Sheets lists its cells
   * and SandFly focuses and speaks the one touched. Returns whether it did.
   */
  public static boolean touchGridIfLost(@Nullable AccessibilityNodeInfoCompat pivot) {
    if (pivot == null
        || isMoving()
        || !TextUtils.equals(pivot.getPackageName(), PACKAGE)
        || cellOf(pivot) != null) {
      return false;
    }
    CharSequence label = pivot.getContentDescription();
    if (label == null || !RANGE.matcher(label).find()) {
      return false;
    }
    AccessibilityNodeInfoCompat grid = pivot.getParent();
    AccessibilityService service = TalkBackService.getInstance();
    if (grid == null || service == null) {
      return false;
    }
    List<AccessibilityNodeInfoCompat> cells = new ArrayList<>();
    if (cellsOf(grid, cells) > 0) {
      return false;
    }
    Rect bounds = new Rect();
    grid.getBoundsInScreen(bounds);
    if (bounds.isEmpty()) {
      return false;
    }
    float x = bounds.exactCenterX();
    float y = bounds.exactCenterY();
    Path still = new Path();
    still.moveTo(x, y);
    still.lineTo(x + 1, y);
    LogUtils.d(TAG, "No cells listed and the focus is on the range label: touching the grid");
    return service.dispatchGesture(
        new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(still, 0, TOUCH_MS))
            .build(),
        null,
        null);
  }

  /** Returns whether the node is a Sheets cell, so row and column navigation work from it. */
  public static boolean isCell(@Nullable AccessibilityNodeInfoCompat node) {
    return cellOf(node) != null;
  }

  /** Returns whether the node belongs to Google Sheets. */
  public static boolean isInSheets(@Nullable AccessibilityNodeInfoCompat node) {
    return node != null && TextUtils.equals(node.getPackageName(), PACKAGE);
  }

  /**
   * Returns whether a focus event comes from a cell passed over during a move and shouldn't be
   * spoken. A touch interaction the user starts stops the move in progress.
   */
  public static boolean shouldDropFocusEvent(AccessibilityEvent event) {
    int type = event.getEventType();
    if (type == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START && !injecting) {
      // What the touch explores is spoken, and a gesture acts from the move's pivot.
      cancel("The screen was touched", /* restorePivot= */ true);
      silentUntil = 0;
      silentTarget = null;
      return false;
    }
    if (quietAddress != null && type == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) {
      String quiet = quietAddress;
      if (SystemClock.uptimeMillis() > quietUntil) {
        quietAddress = null;
      } else if (TextUtils.equals(event.getPackageName(), PACKAGE)) {
        Cell cell = parse(event.getContentDescription());
        if (cell != null && cell.address.equals(quiet)) {
          quietAddress = null;
          return true;
        }
      }
    }
    if (silentUntil == 0) {
      return false;
    }
    boolean scrolling = isScrollingNow();
    if (touching
        && (type == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER
            || type == AccessibilityEvent.TYPE_VIEW_HOVER_EXIT)) {
      return false;
    }
    if (type != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
        && type != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
        && !(scrolling
            && !touching
            && (type == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER
                || type == AccessibilityEvent.TYPE_VIEW_HOVER_EXIT))) {
      return false;
    }
    if (SystemClock.uptimeMillis() > silentUntil) {
      silentUntil = 0;
      silentTarget = null;
      return false;
    }
    if (!TextUtils.equals(event.getPackageName(), PACKAGE)) {
      return false;
    }
    Cell cell = parse(event.getContentDescription());
    if (cell == null) {
      // While the grid scrolls, Sheets may focus its range label or a row header too.
      return scrolling;
    }
    if (type == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
        && cell.address.equals(silentTarget)) {
      silentUntil = 0;
      silentTarget = null;
      return false;
    }
    return true;
  }

  /**
   * Called for each focus action SandFly takes: a focus move by the user (by gesture, key or
   * reading) stops the move in progress, so it doesn't move the focus back.
   */
  public static void onFocusAction(@FocusActionInfo.SourceAction int sourceAction) {
    if (active != null
        && (sourceAction == FocusActionInfo.LOGICAL_NAVIGATION
            || sourceAction == FocusActionInfo.KEYBOARD_SHORTCUT_REFOCUS)) {
      cancel("The focus was moved", /* restorePivot= */ false);
    }
  }

  /** What a step through the cells decides about a cell. */
  interface Goal {
    /** The cell is the target. */
    int FOUND = 0;

    /** Go on past the cell. */
    int CONTINUE = 1;

    /** The target isn't there: stop before the cell. */
    int STOP = 2;

    int test(Cell cell);
  }

  /**
   * Returns what a move of {@code targetType} from {@code from} looks for, or null when there's
   * nothing to look for.
   */
  @Nullable
  static Goal goalFor(@TargetType int targetType, Cell from, boolean forward) {
    switch (targetType) {
      case NavigationTarget.TARGET_ROW -> {
        // Next or previous row, same column.
        int targetRow = from.row + (forward ? 1 : -1);
        if (targetRow < 0) {
          return null;
        }
        return cell -> {
          if (cell.row == targetRow && cell.column == from.column) {
            return Goal.FOUND;
          }
          // Passed the target's place in reading order: that row has no such column.
          boolean beyond = forward ? cell.row > targetRow : cell.row < targetRow;
          boolean pastInRow =
              cell.row == targetRow
                  && (forward ? cell.column > from.column : cell.column < from.column);
          return beyond || pastInRow ? Goal.STOP : Goal.CONTINUE;
        };
      }
      case NavigationTarget.TARGET_COLUMN -> {
        // Next or previous cell in the same row.
        return cell -> cell.row == from.row ? Goal.FOUND : Goal.STOP;
      }
      case NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS -> {
        // First or last cell of the row.
        return cell -> cell.row == from.row ? Goal.CONTINUE : Goal.STOP;
      }
      case NavigationTarget.TARGET_HTML_ELEMENT_COLUMN_BOUNDS,
          NavigationTarget.TARGET_HTML_ELEMENT_TABLE_BOUNDS -> {
        // First or last cell of the column or the table.
        return cell -> Goal.CONTINUE;
      }
      default -> {
        return null;
      }
    }
  }

  /** One table move, from the first step to the cell it ends on. */
  private static final class Move {
    final AccessibilityNodeInfoCompat pivot;
    final Cell from;
    @TargetType final int targetType;
    final boolean forward;

    /** Whether the move goes to the first or last cell of a row, column or the table. */
    final boolean toEnd;

    final Goal goal;
    final PassThrough passThrough;
    final Later later;
    final long start = SystemClock.uptimeMillis();

    /** The cell the steps have reached, focused silently unless it's the pivot. */
    AccessibilityNodeInfoCompat current;

    Cell currentCell;

    /** For moves to an end: the furthest cell found so far that qualifies. */
    @Nullable AccessibilityNodeInfoCompat best;

    @Nullable Cell bestCell;
    int steps = 0;

    /** Until when the current step waits for Sheets to list the cells around the current one. */
    long stepDeadline;

    /** Whether the focus was moved off the pivot. */
    boolean focusedAway = false;

    /** When the move last started running on the main thread. */
    long chunkStart;

    /** Whether {@link #navigate} returned already, so the result goes to {@link #later}. */
    boolean async = false;

    @Nullable AccessibilityNodeInfoCompat result;

    /** The scroll in progress, or null while the move steps through the cells on the screen. */
    @Nullable Scroll scroll;

    /** Whether the pass-through region is set. */
    boolean regionSet = false;

    Move(
        AccessibilityNodeInfoCompat pivot,
        Cell from,
        @TargetType int targetType,
        boolean forward,
        Goal goal,
        PassThrough passThrough,
        Later later) {
      this.pivot = pivot;
      this.from = from;
      this.targetType = targetType;
      this.forward = forward;
      this.toEnd =
          targetType == NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS
              || targetType == NavigationTarget.TARGET_HTML_ELEMENT_COLUMN_BOUNDS
              || targetType == NavigationTarget.TARGET_HTML_ELEMENT_TABLE_BOUNDS;
      this.goal = goal;
      this.passThrough = passThrough;
      this.later = later;
      this.current = pivot;
      this.currentCell = from;
      this.chunkStart = start;
      this.stepDeadline = start + SETTLE_TIMEOUT_MS;
    }

    long elapsed() {
      return SystemClock.uptimeMillis() - start;
    }

    /** Runs {@code part} of the move later, if the move is still in progress then. */
    void post(Runnable part, long delayMs) {
      handler.postDelayed(() -> run(this, part), delayMs);
    }

    void setPassThrough(Region region) {
      passThrough.set(region);
      regionSet = true;
    }

    void clearPassThrough() {
      if (regionSet) {
        regionSet = false;
        passThrough.set(null);
      }
    }
  }

  /**
   * Finds the cell a table move goes to, passing over the cells in between without speaking them.
   * Returns the target cell, for the caller to focus, or null.
   *
   * <p>When the move has to wait for Sheets to list cells, or scrolls the grid, it returns null
   * and {@link #isMoving} is true; the move then gives the cell, or null when there's none in that
   * direction, to {@code later}. A move stopped by the user gives nothing to {@code later}.
   */
  @Nullable
  public static AccessibilityNodeInfoCompat navigate(
      AccessibilityNodeInfoCompat pivot,
      @TargetType int targetType,
      int searchDirection,
      PassThrough passThrough,
      Later later) {
    Cell from = cellOf(pivot);
    if (from == null || isMoving()) {
      return null;
    }
    boolean forward = searchDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD;
    Goal goal = goalFor(targetType, from, forward);
    if (goal == null) {
      return null;
    }
    Move move = new Move(pivot, from, targetType, forward, goal, passThrough, later);
    active = move;
    try {
      step(move);
    } catch (RuntimeException e) {
      LogUtils.w(TAG, "The move from %s failed: %s", from, e);
      fail(move);
    }
    if (active != move) {
      return move.result;
    }
    move.async = true;
    return null;
  }

  /** Returns whether a move is in progress: waiting for Sheets, or scrolling the grid. */
  public static boolean isMoving() {
    Move move = active;
    if (move != null && move.elapsed() > MAX_MOVE_MS) {
      cancel("The move took too long", /* restorePivot= */ false);
      return false;
    }
    return move != null;
  }

  /** Returns whether the move in progress scrolls the grid. */
  private static boolean isScrollingNow() {
    Move move = active;
    return move != null && move.scroll != null;
  }

  /**
   * Steps through the cells towards the move's target. Returns when the move ended, or when it
   * waits for Sheets, having posted the next step.
   */
  private static void step(Move move) {
    boolean columnBounds = move.targetType == NavigationTarget.TARGET_HTML_ELEMENT_COLUMN_BOUNDS;
    try {
      while (move.steps < MAX_STEPS && move.elapsed() < MAX_STEPPING_MS) {
        AccessibilityNodeInfoCompat parent = move.current.getParent();
        Lookup lookup = parent == null ? null : lookNow(parent, move.currentCell, move.forward);
        if (lookup != null
            && !lookup.settled
            && SystemClock.uptimeMillis() < move.stepDeadline) {
          // Sheets hasn't listed the cells around the current one yet.
          move.post(() -> step(move), POLL_MS);
          return;
        }
        AccessibilityNodeInfoCompat next = lookup == null ? null : lookup.found;
        Cell nextCell = cellOf(next);
        if (move.targetType == NavigationTarget.TARGET_COLUMN
            && (nextCell == null || nextCell.row != move.from.row)) {
          // Sheets lists only the columns on the screen, so past the last one the reading order
          // wraps to the next row: slide the grid over and look again.
          if (startScroll(move, /* rows= */ false)) {
            return;
          }
        }
        if (move.targetType == NavigationTarget.TARGET_ROW
            && (nextCell == null || move.goal.test(nextCell) == Goal.STOP)) {
          // The row is off the screen: slide the grid up or down and look again.
          if (startScroll(move, /* rows= */ true)) {
            return;
          }
        }
        if (next == null || nextCell == null) {
          break;
        }
        int decision = move.goal.test(nextCell);
        if (decision == Goal.FOUND) {
          LogUtils.d(TAG, "%s -> %s after %d steps, %d ms", move.from, nextCell, move.steps,
              move.elapsed());
          // Speak the target, which the caller focuses now, also when the end of an earlier
          // move's silence is still running.
          endSilence(nextCell.address);
          complete(move, next);
          return;
        }
        if (decision == Goal.STOP) {
          break;
        }
        if (move.toEnd && (!columnBounds || nextCell.column == move.from.column)) {
          move.best = next;
          move.bestCell = nextCell;
        }
        // Focus the cell silently so Sheets lists the one after it.
        silence(null);
        move.focusedAway = true;
        if (!next.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)) {
          LogUtils.w(TAG, "Couldn't focus %s", nextCell);
          break;
        }
        move.current = next;
        move.currentCell = nextCell;
        move.steps++;
        move.stepDeadline = SystemClock.uptimeMillis() + SETTLE_TIMEOUT_MS;
        if (SystemClock.uptimeMillis() - move.chunkStart > MAX_BLOCK_MS) {
          move.post(() -> step(move), 0);
          return;
        }
      }
    } catch (RuntimeException e) {
      LogUtils.w(TAG, "Stepping through cells failed: %s", e);
    }
    endStepping(move);
  }

  /** Ends a move that stepped as far as it could without finding a target on the way. */
  private static void endStepping(Move move) {
    LogUtils.d(TAG, "%s: stopped at %s after %d steps, %d ms, best=%s", move.from,
        move.currentCell, move.steps, move.elapsed(), move.bestCell);
    if (move.best != null && move.bestCell != null) {
      if (move.best == move.current || move.currentCell == move.bestCell) {
        // Focused silently already: clear it so focusing it again speaks it.
        move.current.performAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
      }
      endSilence(move.bestCell.address);
      complete(move, move.best);
      return;
    }
    endSilence(null);
    if (move.focusedAway) {
      // Nothing found after passing over cells: go back silently to where the move started.
      move.pivot.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
    }
    complete(move, null);
  }

  /**
   * Runs {@code part} of a move posted earlier, if the move is still the one in progress and
   * Sheets is still in front. A failure ends the move.
   */
  private static void run(Move move, Runnable part) {
    if (active != move) {
      return;
    }
    if (move.elapsed() > MAX_MOVE_MS) {
      cancel("The move took too long", /* restorePivot= */ false);
      return;
    }
    if (!TalkBackService.isServiceActive() || otherAppInFront()) {
      cancel("SandFly is suspended or Sheets is no longer in front", /* restorePivot= */ false);
      return;
    }
    move.chunkStart = SystemClock.uptimeMillis();
    try {
      part.run();
    } catch (RuntimeException e) {
      LogUtils.w(TAG, "The move from %s failed: %s", move.from, e);
      fail(move);
    }
  }

  /** Ends the move with no target: the focus goes back silently to where it started. */
  private static void fail(Move move) {
    if (active != move) {
      return;
    }
    endSilence(null);
    if (move.focusedAway || move.scroll != null) {
      try {
        move.pivot.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
      } catch (RuntimeException e) {
        LogUtils.w(TAG, "Couldn't focus %s again: %s", move.from, e);
      }
    }
    complete(move, null);
  }

  /** Ends the move with {@code cell}, the cell to focus and speak, or null. */
  private static void complete(Move move, @Nullable AccessibilityNodeInfoCompat cell) {
    if (active != move) {
      return;
    }
    active = null;
    handler.removeCallbacksAndMessages(null);
    move.clearPassThrough();
    touching = false;
    injecting = false;
    move.result = cell;
    if (move.async) {
      move.later.found(cell);
    }
  }

  /**
   * Stops the move in progress without speaking: the user touched the screen or moved the focus,
   * or left Sheets. With {@code restorePivot}, the focus goes back silently to the cell the move
   * started from, if it's on the screen, so a gesture that follows acts from there and not from a
   * cell passed over; what the touch explores takes the focus afterwards.
   */
  private static void cancel(String reason, boolean restorePivot) {
    Move move = active;
    if (move == null) {
      return;
    }
    LogUtils.d(TAG, "The move from %s stopped: %s", move.from, reason);
    active = null;
    handler.removeCallbacksAndMessages(null);
    move.clearPassThrough();
    touching = false;
    injecting = false;
    silentUntil = 0;
    silentTarget = null;
    if (restorePivot && (move.focusedAway || move.scroll != null)) {
      try {
        if (move.pivot.refresh() && move.pivot.isVisibleToUser()) {
          quietAddress = move.from.address;
          quietUntil = SystemClock.uptimeMillis() + END_SILENCE_MS;
          move.pivot.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
        }
      } catch (RuntimeException e) {
        LogUtils.w(TAG, "Couldn't focus %s again: %s", move.from, e);
      }
    }
  }

  /** Whether the active window belongs to an app other than Sheets. */
  private static boolean otherAppInFront() {
    AccessibilityService service = TalkBackService.getInstance();
    if (service == null) {
      return true;
    }
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    return root != null && !TextUtils.equals(root.getPackageName(), PACKAGE);
  }

  /**
   * Whether Sheets is the active window and the grid is still on the screen, found again if Sheets
   * replaced it.
   */
  private static boolean gridInFront(Scroll scroll) {
    AccessibilityService service = TalkBackService.getInstance();
    AccessibilityNodeInfo root = service == null ? null : service.getRootInActiveWindow();
    if (root == null || !TextUtils.equals(root.getPackageName(), PACKAGE)) {
      return false;
    }
    if (scroll.grid.refresh() && scroll.grid.isVisibleToUser()) {
      return true;
    }
    AccessibilityNodeInfoCompat found = findGrid(scroll.grid);
    if (found == null) {
      return false;
    }
    scroll.grid = found;
    return true;
  }

  /** A move that scrolls the grid. */
  private static final class Scroll {
    final Move move;

    /** The cell the move started from. */
    final Cell origin;

    final boolean forward;

    /** For a row move, the cell it goes to; null for a column move. */
    @Nullable final Cell rowGoal;

    AccessibilityNodeInfoCompat grid;
    final long start = SystemClock.uptimeMillis();
    int scrollsLeft = MAX_SCROLLS;

    /** The cell the move started from, while it's still on the screen. */
    @Nullable AccessibilityNodeInfoCompat originNode;

    /** When the grid last listed no cells and the origin was focused again to list them. */
    long refocusedAt = 0;

    /** Whether the grid was touched to make Sheets list its cells again. */
    boolean touched = false;

    /** How many drags were sent. */
    int drags = 0;

    /** When the window was last searched for the grid. */
    long gridSearchedAt = 0;

    Scroll(Move move, @Nullable Cell rowGoal, AccessibilityNodeInfoCompat grid) {
      this.move = move;
      this.origin = move.from;
      this.forward = move.forward;
      this.rowGoal = rowGoal;
      this.grid = grid;
    }

    /**
     * Whether a cell lies past the origin in the move's direction, in reading order; for a row
     * move, whether it's the goal or past it.
     */
    boolean beyond(Cell cell) {
      return SheetsGrid.beyond(cell, origin, rowGoal, forward);
    }

    /** Whether a cell the walk reached is the one the move goes to. */
    boolean isTarget(Cell cell) {
      return SheetsGrid.isTarget(cell, origin, rowGoal, forward);
    }

    /** The cell to start walking from, among those listed after a scroll. */
    Cell anchor() {
      return rowGoal != null ? rowGoal : origin;
    }

    long elapsed() {
      return SystemClock.uptimeMillis() - start;
    }
  }

  /** See {@link Scroll#beyond}. */
  static boolean beyond(Cell cell, Cell origin, @Nullable Cell rowGoal, boolean forward) {
    if (rowGoal != null) {
      int order = cell.compareTo(rowGoal);
      return forward ? order >= 0 : order <= 0;
    }
    int order = cell.compareTo(origin);
    return forward ? order > 0 : order < 0;
  }

  /** See {@link Scroll#isTarget}. */
  static boolean isTarget(Cell cell, Cell origin, @Nullable Cell rowGoal, boolean forward) {
    if (rowGoal != null) {
      return cell.row == rowGoal.row && cell.column == rowGoal.column;
    }
    return beyond(cell, origin, null, forward) && cell.row == origin.row;
  }

  /**
   * Starts dragging the grid so the row or column the move goes to comes onto the screen; the
   * move then walks to its cell. Returns false if the drag couldn't be started.
   */
  private static boolean startScroll(Move move, boolean rows) {
    AccessibilityNodeInfoCompat grid = move.current.getParent();
    if (grid == null) {
      return false;
    }
    Cell rowGoal = null;
    if (rows) {
      int goalRow = move.from.row + (move.forward ? 1 : -1);
      if (goalRow < 0) {
        return false;
      }
      String letters = move.from.address.replaceAll("[0-9]+$", "");
      rowGoal = new Cell(goalRow, move.from.column, letters + (goalRow + 1));
    } else if (!move.forward && move.from.column == 0) {
      // Nothing comes before column A.
      return false;
    }
    Scroll scroll = new Scroll(move, rowGoal, grid);
    // A failed scroll leaves the focus on the cell the move started from, not on one passed over.
    scroll.originNode = move.pivot;
    move.scroll = scroll;
    if (drag(move.current, scroll)) {
      return true;
    }
    move.scroll = null;
    return false;
  }

  /**
   * Drags the grid by about a column's width: that of {@code cell} or of the widest cell listed,
   * since a cell at the screen's edge may show only a sliver. Returns false if it couldn't be sent.
   */
  private static boolean drag(AccessibilityNodeInfoCompat cell, Scroll scroll) {
    AccessibilityService service = TalkBackService.getInstance();
    if (service == null || scroll.scrollsLeft <= 0 || !gridInFront(scroll)) {
      return false;
    }
    Rect before = new Rect();
    cell.getBoundsInScreen(before);
    Rect gridBounds = new Rect();
    scroll.grid.getBoundsInScreen(gridBounds);
    if (before.isEmpty() || gridBounds.isEmpty()) {
      return false;
    }
    scroll.scrollsLeft--;
    boolean forward = scroll.forward;
    int slop = ViewConfiguration.get(service).getScaledTouchSlop();
    boolean vertical = scroll.rowGoal != null;
    int size = vertical ? before.height() : before.width();
    // The cells' side of the grid, past the row headers: where the first and last listed cells
    // reach (Sheets lists the cell before and after, so one from the next or previous row).
    int cellsLeft = before.left;
    int cellsRight = before.right;
    Rect other = new Rect();
    for (int i = 0; i < scroll.grid.getChildCount(); i++) {
      AccessibilityNodeInfoCompat child = scroll.grid.getChild(i);
      if (cellOf(child) != null) {
        child.getBoundsInScreen(other);
        size = Math.max(size, vertical ? other.height() : other.width());
        cellsLeft = Math.min(cellsLeft, other.left);
        cellsRight = Math.max(cellsRight, other.right);
      }
    }
    // The drag starts next to the cell, one row (or column) towards the middle of the grid:
    // Sheets then focuses that cell and lists the cells around it, so the walk to the next cell
    // afterwards is short. Not on the cell itself: a drag that starts on Sheets' selected cell
    // selects more cells instead of scrolling. A retry after a drag that didn't scroll starts on
    // the other side, then two away.
    int offset = DRAG_OFFSETS[Math.min(scroll.drags, DRAG_OFFSETS.length - 1)];
    scroll.drags++;
    float x = before.exactCenterX();
    float y = before.exactCenterY();
    if (vertical) {
      int towards = x < gridBounds.exactCenterX() ? 1 : -1;
      x += towards * offset * before.width();
    } else {
      int towards = y < gridBounds.exactCenterY() ? 1 : -1;
      y += towards * offset * before.height();
    }
    x = Math.max(cellsLeft + EDGE_MARGIN, Math.min(x, gridBounds.right - EDGE_MARGIN));
    y = Math.max(gridBounds.top + EDGE_MARGIN, Math.min(y, gridBounds.bottom - EDGE_MARGIN));
    int distance;
    int room;
    if (vertical) {
      // Half a screen of rows, so the next moves find their rows on the screen already.
      distance = Math.max(size, gridBounds.height() / 2) + slop;
      room = (int) (forward ? y - gridBounds.top : gridBounds.bottom - y) - EDGE_MARGIN;
    } else {
      // Bring the cell to the far side of the screen, so the columns after it come into view
      // and the next moves don't need to scroll.
      int far = forward ? before.left - cellsLeft : cellsRight - before.right;
      distance = Math.max(size, far) + slop;
      room = (int) (forward ? x - gridBounds.left : gridBounds.right - x) - EDGE_MARGIN;
    }
    distance = Math.min(distance, room);
    if (distance <= slop) {
      return false;
    }
    float endX = vertical ? x : forward ? x - distance : x + distance;
    float endY = !vertical ? y : forward ? y - distance : y + distance;
    LogUtils.d(TAG, "Scrolling %s for %s: cell %s at %s, grid %s, by %d",
        vertical ? (forward ? "down" : "up") : (forward ? "right" : "left"), scroll.origin,
        cellOf(cell), before, gridBounds, distance);
    Path drag = new Path();
    drag.moveTo(x, y);
    drag.lineTo(endX, endY);
    GestureDescription.StrokeDescription stroke =
        new GestureDescription.StrokeDescription(drag, 0, DRAG_MS, /* willContinue= */ true);
    // The finger rests at the end before lifting, so the grid stops instead of flinging on.
    Path rest = new Path();
    rest.moveTo(endX, endY);
    if (vertical) {
      rest.lineTo(endX, endY + (forward ? -1 : 1));
    } else {
      rest.lineTo(endX + (forward ? -1 : 1), endY);
    }
    GestureDescription.StrokeDescription still =
        stroke.continueStroke(rest, 0, REST_MS, /* willContinue= */ false);

    Move move = scroll.move;
    // Sheets focuses the cell the drag starts on; don't speak it.
    silence(null, PASS_THROUGH_WAIT_MS + DRAG_MS + REST_MS + SCROLL_TIMEOUT_MS + RELIST_TIMEOUT_MS);
    // SandFly explores the screen by touch, and injected touches too, so the drag goes through a
    // pass-through strip. A real touch while it's set stops the move, which clears it.
    move.setPassThrough(
        new Region(
            (int) Math.min(x, endX) - PASS_THROUGH_MARGIN,
            (int) Math.min(y, endY) - PASS_THROUGH_MARGIN,
            (int) Math.max(x, endX) + PASS_THROUGH_MARGIN,
            (int) Math.max(y, endY) + PASS_THROUGH_MARGIN));
    Runnable dragged =
        () -> {
          injecting = false;
          move.clearPassThrough();
          waitForScroll(cell, before, scroll, SystemClock.uptimeMillis() + SCROLL_TIMEOUT_MS);
        };
    AccessibilityService.GestureResultCallback rested =
        new AccessibilityService.GestureResultCallback() {
          @Override
          public void onCompleted(GestureDescription description) {
            run(move, dragged);
          }

          @Override
          public void onCancelled(GestureDescription description) {
            gestureCancelled(move, "The rest after the drag");
          }
        };
    AccessibilityService.GestureResultCallback moved =
        new AccessibilityService.GestureResultCallback() {
          @Override
          public void onCompleted(GestureDescription description) {
            // Sent even when the move stopped meanwhile, so the finger lifts.
            boolean sent;
            try {
              sent =
                  service.dispatchGesture(
                      new GestureDescription.Builder().addStroke(still).build(), rested, handler);
            } catch (RuntimeException e) {
              LogUtils.w(TAG, "The rest after the drag wasn't sent: %s", e);
              sent = false;
            }
            if (!sent) {
              run(move, dragged);
            }
          }

          @Override
          public void onCancelled(GestureDescription description) {
            gestureCancelled(move, "The drag");
          }
        };
    move.post(
        () -> {
          if (!gridInFront(scroll)) {
            cancel("Sheets is no longer in front", /* restorePivot= */ false);
            return;
          }
          injecting = true;
          if (!service.dispatchGesture(
              new GestureDescription.Builder().addStroke(stroke).build(), moved, handler)) {
            LogUtils.w(TAG, "The drag wasn't sent");
            dragged.run();
          }
        },
        PASS_THROUGH_WAIT_MS);
    return true;
  }

  /**
   * An injected gesture was cancelled: a finger touched the screen, or another gesture was sent.
   * The move stops.
   */
  private static void gestureCancelled(Move move, String what) {
    if (active == move) {
      cancel(what + " was cancelled", /* restorePivot= */ true);
    }
  }

  /** Waits for the cell to move after a drag. */
  private static void waitForScroll(
      AccessibilityNodeInfoCompat cell, Rect before, Scroll scroll, long deadline) {
    Rect after = new Rect();
    boolean gone = !cell.refresh();
    if (!gone) {
      cell.getBoundsInScreen(after);
    }
    if (!gone && after.equals(before)) {
      if (SystemClock.uptimeMillis() < deadline) {
        scroll.move.post(() -> waitForScroll(cell, before, scroll, deadline), POLL_MS * 2);
        return;
      }
      // A drag on a selected cell selects instead of scrolling: try once more from another cell
      // before calling this the edge.
      if (scroll.drags == 1 && drag(cell, scroll)) {
        LogUtils.d(TAG, "The grid didn't scroll for %s: dragging again", scroll.origin);
        return;
      }
      LogUtils.d(TAG, "The grid didn't scroll: %s is at its edge", scroll.origin);
      finish(scroll, null);
      return;
    }
    LogUtils.d(TAG, "Scrolled after %d ms: %s now at %s", scroll.elapsed(), cellOf(cell),
        gone ? "gone" : after);
    waitForCells(scroll, SystemClock.uptimeMillis() + RELIST_TIMEOUT_MS);
  }

  /**
   * After a scroll, Sheets lists the cells around the one it focused, the one the drag started on.
   * Waits for that list, then walks from there to the cell the move goes to.
   */
  private static void waitForCells(Scroll scroll, long deadline) {
    List<AccessibilityNodeInfoCompat> cells = listedCells(scroll);
    if (cells.isEmpty()) {
      long now = SystemClock.uptimeMillis();
      if (scroll.refocusedAt == 0 && refocusOrigin(scroll)) {
        // Sheets lists cells only around a focused one, and the scroll may have dropped its
        // focus: focus the origin again so it lists the cells around it.
        scroll.refocusedAt = now;
        LogUtils.d(TAG, "No cells listed after scrolling: focused %s again", scroll.origin);
        scroll.move.post(
            () -> waitForCells(scroll, Math.max(deadline, now + RELIST_STEP_MS)), POLL_MS * 2);
        return;
      }
      if (now < deadline) {
        scroll.move.post(() -> waitForCells(scroll, deadline), POLL_MS * 2);
        return;
      }
      LogUtils.w(TAG, "No cells listed after scrolling; grid: %s", describe(scroll.grid));
      if (!scroll.touched && touchToList(scroll)) {
        return;
      }
      finish(scroll, null);
      return;
    }
    AccessibilityNodeInfoCompat anchor = nearest(cells, scroll.anchor(), null);
    String listed = addresses(cells);
    LogUtils.d(TAG, "Listed after scrolling (%d ms): %s", scroll.elapsed(), listed);
    if (anchor == null) {
      finish(scroll, null);
      return;
    }
    // Sheets lists the cells around the focused one again only when the focus moves, and the
    // list may still be the one from before the scroll: focus another cell, then come back, each
    // time waiting for the list to change.
    AccessibilityNodeInfoCompat away = nearest(cells, scroll.anchor(), anchor);
    if (away == null) {
      walk(anchor, cellOf(anchor), scroll);
      return;
    }
    silence(null);
    away.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
    long relistDeadline = SystemClock.uptimeMillis() + RELIST_STEP_MS;
    waitForChange(
        scroll,
        listed,
        relistDeadline,
        afterAway -> {
          AccessibilityNodeInfoCompat back = nearest(afterAway, scroll.anchor(), null);
          if (back == null) {
            back = anchor;
          }
          AccessibilityNodeInfoCompat backNode = back;
          silence(null);
          backNode.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
          waitForChange(
              scroll,
              addresses(afterAway),
              SystemClock.uptimeMillis() + RELIST_STEP_MS,
              afterBack -> {
                AccessibilityNodeInfoCompat from = nearest(afterBack, scroll.anchor(), null);
                if (from == null) {
                  from = backNode;
                }
                walk(from, cellOf(from), scroll);
              });
        });
  }

  /** Something to do with the cells Sheets lists. */
  private interface CellsCallback {
    void run(List<AccessibilityNodeInfoCompat> cells);
  }

  /** Something to do with a cell found, or null. */
  private interface NodeCallback {
    void run(@Nullable AccessibilityNodeInfoCompat node);
  }

  /** Waits for the listed cells to differ from {@code before}, or for the deadline. */
  private static void waitForChange(
      Scroll scroll, String before, long deadline, CellsCallback then) {
    List<AccessibilityNodeInfoCompat> cells = listedCells(scroll);
    String now = addresses(cells);
    if ((cells.isEmpty() || now.equals(before)) && SystemClock.uptimeMillis() < deadline) {
      scroll.move.post(() -> waitForChange(scroll, before, deadline, then), POLL_MS * 2);
      return;
    }
    LogUtils.d(TAG, "Relisted (%d ms): %s -> %s", scroll.elapsed(), before, now);
    then.run(cells);
  }

  /** The cells the grid lists now, finding the grid again if Sheets replaced it. */
  private static List<AccessibilityNodeInfoCompat> listedCells(Scroll scroll) {
    List<AccessibilityNodeInfoCompat> cells = new ArrayList<>();
    boolean present = scroll.grid.refresh();
    if (present && cellsOf(scroll.grid, cells) > 0) {
      return cells;
    }
    // The grid lists no cells, or is gone: search the window, but not on every poll.
    long now = SystemClock.uptimeMillis();
    if (present && now - scroll.gridSearchedAt < GRID_SEARCH_INTERVAL_MS) {
      return cells;
    }
    scroll.gridSearchedAt = now;
    AccessibilityNodeInfoCompat found = findGrid(scroll.grid);
    if (found != null) {
      scroll.grid = found;
      cells.clear();
      cellsOf(found, cells);
    }
    return cells;
  }

  private static int cellsOf(AccessibilityNodeInfoCompat grid, List<AccessibilityNodeInfoCompat> out) {
    for (int i = 0; i < grid.getChildCount(); i++) {
      AccessibilityNodeInfoCompat child = grid.getChild(i);
      if (cellOf(child) != null) {
        out.add(child);
      }
    }
    return out.size();
  }

  /** Looks through the window for a node listing cells, for when Sheets replaced the grid. */
  @Nullable
  private static AccessibilityNodeInfoCompat findGrid(AccessibilityNodeInfoCompat oldGrid) {
    AccessibilityService service = TalkBackService.getInstance();
    if (service == null) {
      return null;
    }
    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null || !TextUtils.equals(root.getPackageName(), PACKAGE)) {
      return null;
    }
    ArrayDeque<AccessibilityNodeInfoCompat> queue = new ArrayDeque<>();
    queue.add(AccessibilityNodeInfoCompat.wrap(root));
    int visited = 0;
    while (!queue.isEmpty() && visited++ < 400) {
      AccessibilityNodeInfoCompat node = queue.poll();
      for (int i = 0; i < node.getChildCount(); i++) {
        AccessibilityNodeInfoCompat child = node.getChild(i);
        if (child == null) {
          continue;
        }
        if (cellOf(child) != null) {
          if (!node.equals(oldGrid)) {
            LogUtils.d(TAG, "Found the grid again: %s", describe(node));
          }
          return node;
        }
        queue.add(child);
      }
    }
    return null;
  }

  /**
   * The cell nearest {@code target} in reading order, preferring its row, other than {@code
   * except}.
   */
  @Nullable
  private static AccessibilityNodeInfoCompat nearest(
      List<AccessibilityNodeInfoCompat> cells,
      Cell target,
      @Nullable AccessibilityNodeInfoCompat except) {
    AccessibilityNodeInfoCompat best = null;
    long bestDistance = Long.MAX_VALUE;
    Cell exceptCell = cellOf(except);
    for (AccessibilityNodeInfoCompat node : cells) {
      Cell c = cellOf(node);
      if (c == null || (exceptCell != null && c.address.equals(exceptCell.address))) {
        continue;
      }
      long d = distance(c, target);
      if (d < bestDistance) {
        best = node;
        bestDistance = d;
      }
    }
    return best;
  }

  private static String addresses(List<AccessibilityNodeInfoCompat> cells) {
    StringBuilder s = new StringBuilder();
    for (AccessibilityNodeInfoCompat node : cells) {
      Cell c = cellOf(node);
      if (c != null) {
        s.append(c.address).append(' ');
      }
    }
    return s.toString().trim();
  }

  private static String describe(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return "null";
    }
    StringBuilder s = new StringBuilder();
    s.append(node.getClassName()).append(" refresh=").append(node.refresh())
        .append(" children=").append(node.getChildCount()).append(" [");
    for (int i = 0; i < node.getChildCount() && i < 8; i++) {
      AccessibilityNodeInfoCompat child = node.getChild(i);
      s.append(child == null ? "null" : child.getContentDescription()).append(" | ");
    }
    return s.append(']').toString();
  }

  /** How far apart two cells are in reading order, counting a row as 1000 columns. */
  private static long distance(Cell a, Cell b) {
    return Math.abs((a.row - b.row) * 1000L + (a.column - b.column));
  }

  /**
   * Steps silently from {@code at} to the first cell past the origin in the move's direction. If
   * that cell is in the origin's row, it's the target; if not, the row's next column is still off
   * the screen, so the grid scrolls again.
   */
  private static void walk(AccessibilityNodeInfoCompat at, @Nullable Cell atCell, Scroll scroll) {
    if (atCell == null) {
      finish(scroll, null);
      return;
    }
    if (scroll.beyond(atCell)) {
      walkBack(scroll, at, atCell, /* focused= */ false, 0);
    } else {
      walkOn(scroll, at, atCell, 0);
    }
  }

  /** Already past the origin: steps back while the cell before is still past it. */
  private static void walkBack(
      Scroll scroll,
      AccessibilityNodeInfoCompat current,
      Cell currentCell,
      boolean focused,
      int steps) {
    if (steps >= MAX_STEPS) {
      walked(scroll, current, currentCell, focused);
      return;
    }
    neighborOrRefocus(
        scroll.move,
        current,
        currentCell,
        !scroll.forward,
        back -> {
          Cell backCell = cellOf(back);
          if (back == null || backCell == null || !scroll.beyond(backCell)) {
            walked(scroll, current, currentCell, focused);
            return;
          }
          silence(null);
          if (!back.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)) {
            walked(scroll, current, currentCell, focused);
            return;
          }
          walkBack(scroll, back, backCell, /* focused= */ true, steps + 1);
        });
  }

  /** Steps towards the origin and past it. */
  private static void walkOn(
      Scroll scroll, AccessibilityNodeInfoCompat current, Cell currentCell, int steps) {
    if (steps >= MAX_STEPS) {
      walked(scroll, current, currentCell, /* focused= */ false);
      return;
    }
    silence(null);
    boolean focused =
        current.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
    neighborOrRefocus(
        scroll.move,
        current,
        currentCell,
        scroll.forward,
        next -> {
          Cell nextCell = cellOf(next);
          if (next == null || nextCell == null) {
            walked(scroll, current, currentCell, focused);
            return;
          }
          if (scroll.beyond(nextCell)) {
            walked(scroll, next, nextCell, /* focused= */ false);
            return;
          }
          walkOn(scroll, next, nextCell, steps + 1);
        });
  }

  /** Ends a walk at {@code current}: the move's target, its end, or a place to scroll on from. */
  private static void walked(
      Scroll scroll, AccessibilityNodeInfoCompat current, Cell currentCell, boolean focused) {
    if (scroll.isTarget(currentCell)) {
      LogUtils.d(TAG, "%s -> %s after scrolling, %d ms", scroll.origin, currentCell,
          scroll.elapsed());
      if (focused) {
        // Focused silently already: clear it so focusing it again speaks it.
        current.performAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
      }
      endSilence(currentCell.address);
      finish(scroll, current);
      return;
    }
    LogUtils.d(TAG, "After scrolling, the cell past %s is %s", scroll.origin, currentCell);
    if (scroll.rowGoal != null && scroll.beyond(currentCell)) {
      // Past the goal's place without finding it: the sheet ends there.
      LogUtils.d(TAG, "No cell %s", scroll.rowGoal);
      if (focused) {
        current.performAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
      }
      finish(scroll, null);
      return;
    }
    // The next column or row is still off the screen: scroll again.
    if (scroll.beyond(currentCell)) {
      awaitNeighbor(
          scroll.move,
          current,
          currentCell,
          !scroll.forward,
          back -> dragOn(scroll, back != null ? back : current));
    } else {
      dragOn(scroll, current);
    }
  }

  /** Scrolls the grid on from {@code edge}, or ends the move. */
  private static void dragOn(Scroll scroll, AccessibilityNodeInfoCompat edge) {
    // Only from the origin's row: if the walk ended in another row, Sheets lists cells far from
    // the origin (a swipe during the scroll moved its focus there), and scrolling on from there
    // would carry the move away from the origin's row.
    Cell edgeCell = cellOf(edge);
    if (edgeCell != null && edgeCell.row == scroll.origin.row && drag(edge, scroll)) {
      return;
    }
    finish(scroll, null);
  }

  /**
   * Like {@link #awaitNeighbor}, but if Sheets lists nothing past {@code current}, focuses it
   * again so Sheets lists the cells around it, and looks once more.
   */
  private static void neighborOrRefocus(
      Move move,
      AccessibilityNodeInfoCompat current,
      Cell currentCell,
      boolean forward,
      NodeCallback then) {
    awaitNeighbor(
        move,
        current,
        currentCell,
        forward,
        found -> {
          if (found != null) {
            then.run(found);
            return;
          }
          silence(null);
          current.performAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
          silence(null);
          if (!current.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)) {
            then.run(null);
            return;
          }
          awaitNeighbor(move, current, currentCell, forward, then);
        });
  }

  /** How long the touch that makes Sheets list its cells again rests on the grid. */
  private static final long TOUCH_MS = 1100;

  /**
   * Sheets lists no cells when none is focused, and focusing one by action may not help (a swipe
   * during a scroll cancels the drag and can leave it so). A finger resting on a cell does: rest
   * an injected touch on the origin (or where it was) long enough for touch exploration, then
   * look for the cells again. Returns false if the touch couldn't be sent.
   */
  private static boolean touchToList(Scroll scroll) {
    AccessibilityService service = TalkBackService.getInstance();
    if (service == null) {
      return false;
    }
    if (!gridInFront(scroll)) {
      cancel("Sheets is no longer in front", /* restorePivot= */ false);
      return true;
    }
    Move move = scroll.move;
    Rect at = new Rect();
    AccessibilityNodeInfoCompat origin = scroll.originNode;
    if (origin != null && origin.refresh()) {
      origin.getBoundsInScreen(at);
    }
    Rect gridBounds = new Rect();
    scroll.grid.getBoundsInScreen(gridBounds);
    if (at.isEmpty() || !Rect.intersects(at, gridBounds)) {
      at.set(gridBounds);
    }
    if (at.isEmpty()) {
      return false;
    }
    scroll.touched = true;
    float x = at.exactCenterX();
    float y = at.exactCenterY();
    Path still = new Path();
    still.moveTo(x, y);
    still.lineTo(x + 1, y);
    LogUtils.d(TAG, "Touching the grid at %.0f,%.0f so Sheets lists its cells", x, y);
    // Touch exploration speaks what's under the finger; keep it quiet.
    silence(null, TOUCH_MS + RELIST_TIMEOUT_MS + 500);
    touching = true;
    injecting = true;
    boolean sent =
        service.dispatchGesture(
            new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(still, 0, TOUCH_MS))
                .build(),
            new AccessibilityService.GestureResultCallback() {
              @Override
              public void onCompleted(GestureDescription description) {
                run(
                    move,
                    () -> {
                      touching = false;
                      injecting = false;
                      waitForCells(scroll, SystemClock.uptimeMillis() + RELIST_TIMEOUT_MS);
                    });
              }

              @Override
              public void onCancelled(GestureDescription description) {
                gestureCancelled(move, "The touch");
              }
            },
            handler);
    if (!sent) {
      touching = false;
      injecting = false;
    }
    return sent;
  }

  /** Focuses the move's origin silently, if it's still on the screen. */
  private static boolean refocusOrigin(Scroll scroll) {
    AccessibilityNodeInfoCompat origin = scroll.originNode;
    if (origin == null || !origin.refresh() || !origin.isVisibleToUser()) {
      return false;
    }
    silence(null);
    return origin.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
  }

  private static void finish(Scroll scroll, @Nullable AccessibilityNodeInfoCompat cell) {
    if (cell == null) {
      // Leave the focus on the cell the move started from, with Sheets listing the cells around
      // it, so the next move starts from there.
      refocusOrigin(scroll);
      endSilence(null);
    }
    complete(scroll.move, cell);
  }

  private static void silence(@Nullable String target) {
    silence(target, 1500);
  }

  /** How long, after a move ends, focus events from the cells passed over may still arrive. */
  private static final long END_SILENCE_MS = 250;

  /**
   * Ends a move's silence: only events already on their way from the cells passed over are
   * dropped, and {@code target}'s focus is spoken; focus the user moves afterwards is spoken. Does
   * nothing when no silence is running.
   */
  static void endSilence(@Nullable String target) {
    if (silentUntil == 0 || SystemClock.uptimeMillis() > silentUntil) {
      silentUntil = 0;
      silentTarget = null;
      return;
    }
    silence(target, END_SILENCE_MS);
  }

  static void silence(@Nullable String target, long durationMs) {
    silentUntil = SystemClock.uptimeMillis() + durationMs;
    silentTarget = target;
  }

  /** What the grid lists past a cell. */
  private static final class Lookup {
    @Nullable final AccessibilityNodeInfoCompat found;

    /**
     * Whether the grid lists the cell itself and one past it, so it lists the cells around it
     * already.
     */
    final boolean settled;

    Lookup(@Nullable AccessibilityNodeInfoCompat found, boolean settled) {
      this.found = found;
      this.settled = settled;
    }
  }

  /** Returns what {@code parent} lists just after or before {@code currentCell}, without waiting. */
  private static Lookup lookNow(
      AccessibilityNodeInfoCompat parent, Cell currentCell, boolean forward) {
    parent.refresh();
    List<AccessibilityNodeInfoCompat> nodes = new ArrayList<>();
    List<Cell> cells = new ArrayList<>();
    for (int i = 0; i < parent.getChildCount(); i++) {
      AccessibilityNodeInfoCompat child = parent.getChild(i);
      Cell cell = cellOf(child);
      if (cell != null) {
        nodes.add(child);
        cells.add(cell);
      }
    }
    int index = nextIndex(cells, currentCell, forward);
    if (index < 0) {
      return new Lookup(null, false);
    }
    return new Lookup(nodes.get(index), lists(cells, currentCell));
  }

  /**
   * Returns the index of the cell just after (or before) {@code current} in reading order, or -1
   * when none of {@code cells} is past it.
   */
  static int nextIndex(List<Cell> cells, Cell current, boolean forward) {
    int found = -1;
    for (int i = 0; i < cells.size(); i++) {
      Cell cell = cells.get(i);
      int order = cell.compareTo(current);
      if (forward ? order <= 0 : order >= 0) {
        continue;
      }
      if (found < 0
          || (forward
              ? cell.compareTo(cells.get(found)) < 0
              : cell.compareTo(cells.get(found)) > 0)) {
        found = i;
      }
    }
    return found;
  }

  /** Returns whether {@code cells} has {@code cell}'s address. */
  static boolean lists(List<Cell> cells, Cell cell) {
    for (Cell c : cells) {
      if (c.compareTo(cell) == 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Gives {@code then} the cell just after or before {@code current} in reading order among the
   * cells Sheets lists, once Sheets lists the cells around it, or what it lists after {@link
   * #SETTLE_TIMEOUT_MS}. Waits by posting, not on the main thread.
   */
  private static void awaitNeighbor(
      Move move,
      AccessibilityNodeInfoCompat current,
      Cell currentCell,
      boolean forward,
      NodeCallback then) {
    AccessibilityNodeInfoCompat parent = current.getParent();
    if (parent == null) {
      then.run(null);
      return;
    }
    awaitNeighbor(
        move, parent, currentCell, forward, SystemClock.uptimeMillis() + SETTLE_TIMEOUT_MS, then);
  }

  private static void awaitNeighbor(
      Move move,
      AccessibilityNodeInfoCompat parent,
      Cell currentCell,
      boolean forward,
      long deadline,
      NodeCallback then) {
    Lookup lookup = lookNow(parent, currentCell, forward);
    long now = SystemClock.uptimeMillis();
    if (!lookup.settled && now < deadline) {
      move.post(() -> awaitNeighbor(move, parent, currentCell, forward, deadline, then), POLL_MS);
      return;
    }
    if (now - move.chunkStart > MAX_BLOCK_MS) {
      // Let other work run before the next step.
      move.post(() -> then.run(lookup.found), 0);
      return;
    }
    then.run(lookup.found);
  }
}
