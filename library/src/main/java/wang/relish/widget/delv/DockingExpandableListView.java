package wang.relish.widget.delv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static wang.relish.widget.delv.DockingExpandableListView.DockStatus.DOCKED;
import static wang.relish.widget.delv.DockingExpandableListView.DockStatus.DOCKING;
import static wang.relish.widget.delv.DockingExpandableListView.DockStatus.HIDDEN;

/**
 * 带头部悬停的ExpandableListView
 *
 * @author qianxin, relish
 * @since 20161121
 */
public class DockingExpandableListView extends ExpandableListView implements OnScrollListener {
    private View mDockingHeader;
    private int mDockingHeaderWidth;
    private int mDockingHeaderHeight;
    private boolean mDockingHeaderVisible;
    @DockStatus
    private int mDockingHeaderState = HIDDEN;

    public DockingExpandableListView(Context context) {
        this(context, null);
    }

    public DockingExpandableListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DockingExpandableListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOnScrollListener(this);
    }

    @Override
    public void setAdapter(ExpandableListAdapter adapter) {
        super.setAdapter(adapter);

        if (adapter == null) return;
        if (adapter instanceof DockingExpandableListViewAdapter) {
            final int layoutId = ((DockingExpandableListViewAdapter) adapter).dockingHeaderResId();
            mDockingHeader = LayoutInflater.from(getContext()).inflate(
                    layoutId, this, false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (mDockingHeader != null) {
            measureChild(mDockingHeader, widthMeasureSpec, heightMeasureSpec);
            mDockingHeaderWidth = mDockingHeader.getMeasuredWidth();
            mDockingHeaderHeight = mDockingHeader.getMeasuredHeight();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mDockingHeader != null) {
            mDockingHeader.layout(0, 0, mDockingHeaderWidth, mDockingHeaderHeight);
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mDockingHeaderVisible) {
            // draw header view instead of adding into view hierarchy
            drawChild(canvas, mDockingHeader, getDrawingTime());
        }
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        long packedPosition = getExpandableListPosition(firstVisibleItem);
        int groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition);
        int childPosition = ExpandableListView.getPackedPositionChild(packedPosition);

        // update header view based on first visible item
        // IMPORTANT: refer to getPackedPositionChild():
        // If this group does not contain a child, returns -1. Need to handle this case in controller.
        updateDockingHeader(groupPosition, childPosition);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    private void updateDockingHeader(int groupPosition, int childPosition) {
        final ExpandableListAdapter adapter = getExpandableListAdapter();
        if (adapter == null) return;
        if (!(adapter instanceof DockingExpandableListViewAdapter)) return;
        mDockingHeaderState = getDockingState(groupPosition, childPosition);
        switch (mDockingHeaderState) {
            case DockStatus.HIDDEN:
            mDockingHeaderVisible = false;
            break;
            case DOCKED:
            adapter.getGroupView(
                    groupPosition,
                    isGroupExpanded(groupPosition),
                    mDockingHeader,
                    this
            );
            // Header view might be "GONE" status at the beginning, so we might not be able
            // to get its width and height during initial measure procedure.
            // Do manual measure and layout operations here.
            mDockingHeader.measure(
                    MeasureSpec.makeMeasureSpec(mDockingHeaderWidth, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(mDockingHeaderHeight, MeasureSpec.AT_MOST));
            mDockingHeader.layout(0, 0, mDockingHeaderWidth, mDockingHeaderHeight);
            mDockingHeaderVisible = true;
            break;
            case DockStatus.DOCKING:
            adapter.getGroupView(
                    groupPosition,
                    isGroupExpanded(groupPosition),
                    mDockingHeader,
                    this
            );

            View firstVisibleView = getChildAt(0);
            int yOffset;
            if (firstVisibleView.getBottom() < mDockingHeaderHeight) {
                yOffset = firstVisibleView.getBottom() - mDockingHeaderHeight;
            } else {
                yOffset = 0;
            }

            // The yOffset is always non-positive. When a new header view is "docking",
            // previous header view need to be "scrolled over". Thus we need to draw the
            // old header view based on last child's scroll amount.
            mDockingHeader.measure(
                    MeasureSpec.makeMeasureSpec(mDockingHeaderWidth, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(mDockingHeaderHeight, MeasureSpec.AT_MOST));
            mDockingHeader.layout(0, yOffset, mDockingHeaderWidth, mDockingHeaderHeight + yOffset);
            mDockingHeaderVisible = true;
            break;
        }
    }

    @DockStatus
    private int getDockingState(int firstVisibleGroup, int firstVisibleChild) {
        // No need to draw header view if this group does not contain any child & also not expanded.
        if (firstVisibleChild == -1 && !isGroupExpanded(firstVisibleGroup)) {
            return DockStatus.HIDDEN;
        }

        // Reaching current group's last child, preparing for docking next group header.
        final ExpandableListAdapter adapter = getExpandableListAdapter();
        if (firstVisibleChild == adapter.getChildrenCount(firstVisibleGroup) - 1) {
            return DockStatus.DOCKING;
        }

        // Scrolling inside current group, header view is docked.
        return DockStatus.DOCKED;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN && mDockingHeaderVisible) {
            Rect rect = new Rect();
            mDockingHeader.getDrawingRect(rect);
            if (rect.contains((int) ev.getX(), (int) ev.getY())
                    && mDockingHeaderState == DOCKED) {
                // Hit header view area, intercept the touch event
                return true;
            }
        }

        return super.onInterceptTouchEvent(ev);
    }

    // Note: As header view is drawn to the canvas instead of adding into view hierarchy,
    // it's useless to set its touch or click event listener. Need to handle these input
    // events carefully by ourselves.
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mDockingHeaderVisible) {
            Rect rect = new Rect();
            mDockingHeader.getDrawingRect(rect);

            switch (ev.getAction()) {
                case MotionEvent.ACTION_DOWN:
                if (rect.contains((int) ev.getX(), (int) ev.getY())) {
                    // forbid event handling by list view's item
                    return true;
                }
                break;
                case MotionEvent.ACTION_UP:
                long flatPosition = getExpandableListPosition(getFirstVisiblePosition());
                int groupPos = ExpandableListView.getPackedPositionGroup(flatPosition);
                if (rect.contains((int) ev.getX(), (int) ev.getY()) &&
                        mDockingHeaderState == DOCKED) {
                    // handle header view click event (do group expansion & collapse)
                    if (isGroupExpanded(groupPos)) {
                        collapseGroup(groupPos);
                    } else {
                        expandGroup(groupPos);
                    }
                    performClick();
                    return true;
                }
                break;
            }
        }

        return super.onTouchEvent(ev);
    }

    @IntDef({HIDDEN, DOCKING, DOCKED})
    @Retention(RetentionPolicy.SOURCE)
    @interface DockStatus {
        int HIDDEN = 1;
        int DOCKING = 2;
        int DOCKED = 3;
    }
}
