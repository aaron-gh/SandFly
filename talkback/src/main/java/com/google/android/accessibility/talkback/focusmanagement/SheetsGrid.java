package com.google.android.accessibility.talkback.focusmanagement;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.ViewConfiguration;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TargetType;
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

  private static final Handler handler = new Handler(Looper.getMainLooper());

  /** Whether a column move is waiting for the grid to scroll. */
  private static boolean scrolling = false;

  /**
   * Whether an injected touch rests on the grid so Sheets lists its cells again: its hover events
   * must reach SandFly, which focuses the cell under it, so they aren't dropped.
   */
  private static boolean touching = false;

  /** Lets touches through to the app in a region of the screen, or stops that when null. */
  public interface PassThrough {
    void set(@Nullable Region region);
  }

  /** Gets the cell a column move found after scrolling the grid, or null when there was none. */
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
        || scrolling
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

  /**
   * Returns whether a focus event comes from a cell passed over during a move and shouldn't be
   * spoken.
   */
  public static boolean shouldDropFocusEvent(AccessibilityEvent event) {
    if (silentUntil == 0) {
      return false;
    }
    int type = event.getEventType();
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

  /** What a step through the cells decides about a cell. */
  private interface Goal {
    /** The cell is the target. */
    int FOUND = 0;

    /** Go on past the cell. */
    int CONTINUE = 1;

    /** The target isn't there: stop before the cell. */
    int STOP = 2;

    int test(Cell cell);
  }

  /**
   * Finds the cell a table move goes to, passing over the cells in between without speaking them.
   * Returns the target cell, for the caller to focus, or null when there's none in that direction.
   *
   * <p>A column move past the last (or first) column on the screen returns null and scrolls the
   * grid sideways, then gives the cell to {@code later}; {@link #isScrolling} is true meanwhile.
   */
  @Nullable
  public static AccessibilityNodeInfoCompat navigate(
      AccessibilityNodeInfoCompat pivot,
      @TargetType int targetType,
      int searchDirection,
      PassThrough passThrough,
      Later later) {
    Cell from = cellOf(pivot);
    if (from == null || scrolling) {
      return null;
    }
    boolean forward = searchDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD;
    boolean toEnd;
    Goal goal;
    switch (targetType) {
      case NavigationTarget.TARGET_ROW -> {
        // Next or previous row, same column.
        int targetRow = from.row + (forward ? 1 : -1);
        if (targetRow < 0) {
          return null;
        }
        toEnd = false;
        goal =
            cell -> {
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
        toEnd = false;
        goal = cell -> cell.row == from.row ? Goal.FOUND : Goal.STOP;
      }
      case NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS -> {
        // First or last cell of the row.
        toEnd = true;
        goal = cell -> cell.row == from.row ? Goal.CONTINUE : Goal.STOP;
      }
      case NavigationTarget.TARGET_HTML_ELEMENT_COLUMN_BOUNDS -> {
        // First or last cell of the column.
        toEnd = true;
        goal = cell -> Goal.CONTINUE;
      }
      case NavigationTarget.TARGET_HTML_ELEMENT_TABLE_BOUNDS -> {
        toEnd = true;
        goal = cell -> Goal.CONTINUE;
      }
      default -> {
        return null;
      }
    }
    boolean columnBounds = targetType == NavigationTarget.TARGET_HTML_ELEMENT_COLUMN_BOUNDS;
    long start = SystemClock.uptimeMillis();
    AccessibilityNodeInfoCompat current = pivot;
    Cell currentCell = from;
    // For moves to an end: the furthest cell found so far that qualifies.
    @Nullable AccessibilityNodeInfoCompat best = null;
    @Nullable Cell bestCell = null;
    int steps = 0;
    try {
      while (steps < MAX_STEPS) {
        AccessibilityNodeInfoCompat next = neighbor(current, currentCell, forward);
        Cell nextCell = next == null ? null : cellOf(next);
        if (targetType == NavigationTarget.TARGET_COLUMN
            && (nextCell == null || nextCell.row != from.row)) {
          // Sheets lists only the columns on the screen, so past the last one the reading order
          // wraps to the next row: slide the grid over and look again.
          if (startScroll(current, from, forward, passThrough, later)) {
            return null;
          }
        }
        if (targetType == NavigationTarget.TARGET_ROW
            && (nextCell == null || goal.test(nextCell) == Goal.STOP)) {
          // The row is off the screen: slide the grid up or down and look again.
          if (startRowScroll(current, from, forward, passThrough, later)) {
            return null;
          }
        }
        if (next == null) {
          break;
        }
        int decision = goal.test(nextCell);
        if (decision == Goal.FOUND) {
          LogUtils.d(TAG, "%s -> %s after %d steps, %d ms", from, nextCell, steps,
              SystemClock.uptimeMillis() - start);
          if (steps > 0) {
            // Cells were passed over silently: speak the target, which the caller focuses now.
            endSilence(nextCell.address);
          }
          return next;
        }
        if (decision == Goal.STOP) {
          break;
        }
        if (toEnd && (!columnBounds || nextCell.column == from.column)) {
          best = next;
          bestCell = nextCell;
        }
        // Focus the cell silently so Sheets lists the one after it.
        silence(null);
        if (!next.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)) {
          LogUtils.w(TAG, "Couldn't focus %s", nextCell);
          break;
        }
        current = next;
        currentCell = nextCell;
        steps++;
      }
    } catch (RuntimeException e) {
      LogUtils.w(TAG, "Stepping through cells failed: %s", e);
    }
    LogUtils.d(TAG, "%s: stopped at %s after %d steps, %d ms, best=%s", from, currentCell, steps,
        SystemClock.uptimeMillis() - start, bestCell);
    if (best != null) {
      if (best == current || currentCell == bestCell) {
        // Focused silently already: clear it so focusing it again speaks it.
        current.performAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
      }
      endSilence(bestCell.address);
      return best;
    }
    if (steps > 0) {
      // Nothing found after passing over cells: go back silently to where the move started.
      endSilence(null);
      pivot.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
    }
    return null;
  }

  /** Returns whether a column move is waiting for the grid to scroll. */
  public static boolean isScrolling() {
    return scrolling;
  }

  /** A column move that scrolls the grid sideways. */
  private static final class Scroll {
    /** The cell the move started from. */
    final Cell origin;

    final boolean forward;

    /** For a row move, the cell it goes to; null for a column move. */
    @Nullable final Cell rowGoal;

    AccessibilityNodeInfoCompat grid;
    final PassThrough passThrough;
    final Later later;
    final long start = SystemClock.uptimeMillis();
    int scrollsLeft = MAX_SCROLLS;

    /** The cell the last drag was measured from. */
    @Nullable AccessibilityNodeInfoCompat lastCell;

    /** The cell the move started from, while it's still on the screen. */
    @Nullable AccessibilityNodeInfoCompat originNode;

    /** When the grid last listed no cells and the origin was focused again to list them. */
    long refocusedAt = 0;

    /** Whether the grid was touched to make Sheets list its cells again. */
    boolean touched = false;

    /** How many times the touch was cancelled and sent again. */
    int touchRetries = 0;

    /** How many drags were sent. */
    int drags = 0;

    Scroll(
        Cell origin,
        boolean forward,
        @Nullable Cell rowGoal,
        AccessibilityNodeInfoCompat grid,
        PassThrough passThrough,
        Later later) {
      this.origin = origin;
      this.forward = forward;
      this.rowGoal = rowGoal;
      this.grid = grid;
      this.passThrough = passThrough;
      this.later = later;
    }

    /**
     * Whether a cell lies past the origin in the move's direction, in reading order; for a row
     * move, whether it's the goal or past it.
     */
    boolean beyond(Cell cell) {
      if (rowGoal != null) {
        int order = cell.compareTo(rowGoal);
        return forward ? order >= 0 : order <= 0;
      }
      int order = cell.compareTo(origin);
      return forward ? order > 0 : order < 0;
    }

    /** Whether a cell the walk reached is the one the move goes to. */
    boolean isTarget(Cell cell) {
      if (rowGoal != null) {
        return cell.row == rowGoal.row && cell.column == rowGoal.column;
      }
      return beyond(cell) && cell.row == origin.row;
    }

    /** The cell to start walking from, among those listed after a scroll. */
    Cell anchor() {
      return rowGoal != null ? rowGoal : origin;
    }

    long elapsed() {
      return SystemClock.uptimeMillis() - start;
    }
  }

  /**
   * Starts dragging the grid by about one column so the column after (or before) {@code cell}
   * comes onto the screen, then gives the next cell in the row to {@code later}. Returns false if
   * the drag couldn't be started.
   */
  private static boolean startScroll(
      AccessibilityNodeInfoCompat cell,
      Cell address,
      boolean forward,
      PassThrough passThrough,
      Later later) {
    AccessibilityNodeInfoCompat grid = cell.getParent();
    if (grid == null || (!forward && address.column == 0)) {
      // Nothing comes before column A.
      return false;
    }
    Scroll scroll = new Scroll(address, forward, null, grid, passThrough, later);
    scroll.originNode = cell;
    return drag(cell, scroll);
  }

  /**
   * Starts dragging the grid by about one row so the row after (or before) {@code cell}'s comes
   * onto the screen, then gives the cell in the origin's column to {@code later}.
   */
  private static boolean startRowScroll(
      AccessibilityNodeInfoCompat cell,
      Cell origin,
      boolean forward,
      PassThrough passThrough,
      Later later) {
    AccessibilityNodeInfoCompat grid = cell.getParent();
    int goalRow = origin.row + (forward ? 1 : -1);
    if (grid == null || goalRow < 0) {
      return false;
    }
    String letters = origin.address.replaceAll("[0-9]+$", "");
    Cell goal = new Cell(goalRow, origin.column, letters + (goalRow + 1));
    Scroll scroll = new Scroll(origin, forward, goal, grid, passThrough, later);
    scroll.originNode = cell;
    return drag(cell, scroll);
  }

  /**
   * Drags the grid by about a column's width: that of {@code cell} or of the widest cell listed,
   * since a cell at the screen's edge may show only a sliver. Returns false if it couldn't be sent.
   */
  private static boolean drag(AccessibilityNodeInfoCompat cell, Scroll scroll) {
    AccessibilityService service = TalkBackService.getInstance();
    if (service == null || scroll.scrollsLeft <= 0) {
      return false;
    }
    Rect before = new Rect();
    cell.getBoundsInScreen(before);
    Rect gridBounds = new Rect();
    scroll.grid.refresh();
    scroll.grid.getBoundsInScreen(gridBounds);
    if (before.isEmpty() || gridBounds.isEmpty()) {
      return false;
    }
    scroll.scrollsLeft--;
    scroll.lastCell = cell;
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

    scrolling = true;
    // Sheets focuses the cell the drag starts on; don't speak it.
    silence(null, PASS_THROUGH_WAIT_MS + DRAG_MS + REST_MS + SCROLL_TIMEOUT_MS + RELIST_TIMEOUT_MS);
    // SandFly explores the screen by touch, and injected touches too, so the drag goes through a
    // pass-through strip.
    scroll.passThrough.set(
        new Region(
            (int) Math.min(x, endX) - PASS_THROUGH_MARGIN,
            (int) Math.min(y, endY) - PASS_THROUGH_MARGIN,
            (int) Math.max(x, endX) + PASS_THROUGH_MARGIN,
            (int) Math.max(y, endY) + PASS_THROUGH_MARGIN));
    Runnable dragged =
        () -> {
          scroll.passThrough.set(null);
          waitForScroll(
              cell, before, scroll, SystemClock.uptimeMillis() + SCROLL_TIMEOUT_MS);
        };
    AccessibilityService.GestureResultCallback rested =
        new AccessibilityService.GestureResultCallback() {
          @Override
          public void onCompleted(GestureDescription description) {
            dragged.run();
          }

          @Override
          public void onCancelled(GestureDescription description) {
            LogUtils.w(TAG, "The rest after the drag was cancelled");
            dragged.run();
          }
        };
    AccessibilityService.GestureResultCallback moved =
        new AccessibilityService.GestureResultCallback() {
          @Override
          public void onCompleted(GestureDescription description) {
            if (!service.dispatchGesture(
                new GestureDescription.Builder().addStroke(still).build(), rested, handler)) {
              dragged.run();
            }
          }

          @Override
          public void onCancelled(GestureDescription description) {
            LogUtils.w(TAG, "The drag was cancelled");
            dragged.run();
          }
        };
    handler.postDelayed(
        () -> {
          if (!service.dispatchGesture(
              new GestureDescription.Builder().addStroke(stroke).build(), moved, handler)) {
            LogUtils.w(TAG, "The drag wasn't sent");
            dragged.run();
          }
        },
        PASS_THROUGH_WAIT_MS);
    return true;
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
        handler.postDelayed(() -> waitForScroll(cell, before, scroll, deadline), POLL_MS * 2);
        return;
      }
      // A swipe during the drag cancels it, and a drag on a selected cell selects instead of
      // scrolling: try once more from another cell before calling this the edge.
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
        handler.postDelayed(
            () -> waitForCells(scroll, Math.max(deadline, now + RELIST_STEP_MS)), POLL_MS * 2);
        return;
      }
      if (now < deadline) {
        handler.postDelayed(() -> waitForCells(scroll, deadline), POLL_MS * 2);
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

  /** Waits for the listed cells to differ from {@code before}, or for the deadline. */
  private static void waitForChange(
      Scroll scroll, String before, long deadline, CellsCallback then) {
    List<AccessibilityNodeInfoCompat> cells = listedCells(scroll);
    String now = addresses(cells);
    if ((cells.isEmpty() || now.equals(before)) && SystemClock.uptimeMillis() < deadline) {
      handler.postDelayed(() -> waitForChange(scroll, before, deadline, then), POLL_MS * 2);
      return;
    }
    LogUtils.d(TAG, "Relisted (%d ms): %s -> %s", scroll.elapsed(), before, now);
    then.run(cells);
  }

  /** The cells the grid lists now, finding the grid again if Sheets replaced it. */
  private static List<AccessibilityNodeInfoCompat> listedCells(Scroll scroll) {
    List<AccessibilityNodeInfoCompat> cells = new ArrayList<>();
    if (!scroll.grid.refresh() || cellsOf(scroll.grid, cells) == 0) {
      AccessibilityNodeInfoCompat found = findGrid(scroll.grid);
      if (found != null) {
        scroll.grid = found;
        cells.clear();
        cellsOf(found, cells);
      }
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
    android.view.accessibility.AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null) {
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
  private static void walk(AccessibilityNodeInfoCompat at, Cell atCell, Scroll scroll) {
    boolean forward = scroll.forward;
    AccessibilityNodeInfoCompat current = at;
    Cell currentCell = atCell;
    boolean focused = false;
    try {
      if (scroll.beyond(currentCell)) {
        // Already past the origin: step back while the cell before is still past it.
        for (int steps = 0; steps < MAX_STEPS; steps++) {
          AccessibilityNodeInfoCompat back = neighborOrRefocus(current, currentCell, !forward);
          Cell backCell = cellOf(back);
          if (backCell == null || !scroll.beyond(backCell)) {
            break;
          }
          silence(null);
          if (!back.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)) {
            break;
          }
          current = back;
          currentCell = backCell;
          focused = true;
        }
      } else {
        // Step towards the origin and past it.
        for (int steps = 0; steps < MAX_STEPS; steps++) {
          silence(null);
          current.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
          AccessibilityNodeInfoCompat next = neighborOrRefocus(current, currentCell, forward);
          Cell nextCell = cellOf(next);
          if (nextCell == null) {
            break;
          }
          current = next;
          currentCell = nextCell;
          focused = false;
          if (scroll.beyond(nextCell)) {
            break;
          }
        }
      }
    } catch (RuntimeException e) {
      LogUtils.w(TAG, "Walking through cells failed: %s", e);
    }
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
    AccessibilityNodeInfoCompat edge = current;
    if (scroll.beyond(currentCell)) {
      AccessibilityNodeInfoCompat back = neighbor(current, currentCell, !forward);
      if (back != null) {
        edge = back;
      }
    }
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
   * Like {@link #neighbor}, but if Sheets lists nothing past {@code current}, focuses it again so
   * Sheets lists the cells around it, and looks once more.
   */
  @Nullable
  private static AccessibilityNodeInfoCompat neighborOrRefocus(
      AccessibilityNodeInfoCompat current, Cell currentCell, boolean forward) {
    AccessibilityNodeInfoCompat found = neighbor(current, currentCell, forward);
    if (found != null) {
      return found;
    }
    silence(null);
    current.performAction(AccessibilityNodeInfoCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS);
    silence(null);
    if (!current.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS)) {
      return null;
    }
    return neighbor(current, currentCell, forward);
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
    Runnable after =
        () -> {
          touching = false;
          waitForCells(scroll, SystemClock.uptimeMillis() + RELIST_TIMEOUT_MS);
        };
    boolean sent =
        service.dispatchGesture(
            new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(still, 0, TOUCH_MS))
                .build(),
            new AccessibilityService.GestureResultCallback() {
              @Override
              public void onCompleted(GestureDescription description) {
                after.run();
              }

              @Override
              public void onCancelled(GestureDescription description) {
                // A finger on the screen cancels it; try again once that's done.
                LogUtils.d(TAG, "The touch was cancelled");
                touching = false;
                if (scroll.touchRetries++ < 2) {
                  scroll.touched = false;
                  handler.postDelayed(
                      () -> {
                        if (!touchToList(scroll)) {
                          finish(scroll, null);
                        }
                      },
                      400);
                } else {
                  after.run();
                }
              }
            },
            handler);
    if (!sent) {
      touching = false;
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
    boolean focused = origin.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
    return focused;
  }

  private static void finish(Scroll scroll, @Nullable AccessibilityNodeInfoCompat cell) {
    if (cell == null) {
      // Leave the focus on the cell the move started from, with Sheets listing the cells around
      // it, so the next move starts from there.
      refocusOrigin(scroll);
      endSilence(null);
    }
    scrolling = false;
    scroll.later.found(cell);
  }

  private static void silence(@Nullable String target) {
    silence(target, 1500);
  }

  /** How long, after a move ends, focus events from the cells passed over may still arrive. */
  private static final long END_SILENCE_MS = 250;

  /**
   * Ends a move's silence: only events already on their way from the cells passed over are
   * dropped, and {@code target}'s focus is spoken; focus the user moves afterwards is spoken.
   */
  private static void endSilence(@Nullable String target) {
    silence(target, END_SILENCE_MS);
  }

  private static void silence(@Nullable String target, long durationMs) {
    silentUntil = SystemClock.uptimeMillis() + durationMs;
    silentTarget = target;
  }

  /**
   * Returns the cell just after or before {@code current} in reading order among the cells Sheets
   * lists, waiting a moment for Sheets to list them after a focus change.
   */
  @Nullable
  private static AccessibilityNodeInfoCompat neighbor(
      AccessibilityNodeInfoCompat current, Cell currentCell, boolean forward) {
    AccessibilityNodeInfoCompat parent = current.getParent();
    if (parent == null) {
      return null;
    }
    long deadline = SystemClock.uptimeMillis() + SETTLE_TIMEOUT_MS;
    while (true) {
      parent.refresh();
      AccessibilityNodeInfoCompat found = null;
      Cell foundCell = null;
      boolean listsCurrent = false;
      for (int i = 0; i < parent.getChildCount(); i++) {
        AccessibilityNodeInfoCompat child = parent.getChild(i);
        Cell cell = cellOf(child);
        if (cell == null) {
          continue;
        }
        int order = cell.compareTo(currentCell);
        if (order == 0) {
          listsCurrent = true;
          continue;
        }
        if (forward ? order > 0 : order < 0) {
          if (foundCell == null || (forward ? cell.compareTo(foundCell) < 0 : cell.compareTo(foundCell) > 0)) {
            found = child;
            foundCell = cell;
          }
        }
      }
      if (found != null && listsCurrent) {
        return found;
      }
      if (SystemClock.uptimeMillis() >= deadline) {
        // Sheets lists the cells around the focused one; with the current one listed and nothing
        // past it, this is the grid's edge.
        return found;
      }
      SystemClock.sleep(POLL_MS);
    }
  }
}
