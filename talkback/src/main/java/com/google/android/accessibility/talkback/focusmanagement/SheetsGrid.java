package com.google.android.accessibility.talkback.focusmanagement;

import android.os.SystemClock;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TargetType;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
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

  /** Focus events from cells passed over are dropped until this time. */
  private static long silentUntil = 0;

  /** The address to speak at the end of a move, the only cell not dropped while silent. */
  @Nullable private static String silentTarget = null;

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
    if (type != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
        && type != AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED) {
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
      return false;
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
   */
  @Nullable
  public static AccessibilityNodeInfoCompat navigate(
      AccessibilityNodeInfoCompat pivot, @TargetType int targetType, int searchDirection) {
    Cell from = cellOf(pivot);
    if (from == null) {
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
        if (next == null) {
          break;
        }
        Cell nextCell = cellOf(next);
        int decision = goal.test(nextCell);
        if (decision == Goal.FOUND) {
          LogUtils.d(TAG, "%s -> %s after %d steps, %d ms", from, nextCell, steps,
              SystemClock.uptimeMillis() - start);
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
      silence(bestCell.address);
      return best;
    }
    if (steps > 0) {
      // Nothing found after passing over cells: go back silently to where the move started.
      silence(null);
      pivot.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);
    }
    return null;
  }

  private static void silence(@Nullable String target) {
    silentUntil = SystemClock.uptimeMillis() + 1500;
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
