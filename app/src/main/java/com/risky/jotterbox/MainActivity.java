package com.risky.jotterbox;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.risky.jotterbox.dao.Board;
import com.risky.jotterbox.dao.Note;
import com.risky.jotterbox.data.ObjectBoxSettingManager;
import com.risky.jotterbox.fragment.dialog.BoardEditDialogFragment;
import com.risky.jotterbox.fragment.dialog.SelectNoteTypeDialogFragment;
import com.risky.jotterbox.struct.Point2D;
import com.risky.jotterbox.utils.DeviceProperties;
import com.risky.jotterbox.worldobject.BoardFragment;
import com.risky.jotterbox.objectbox.ObjectBox;

import java.util.concurrent.CompletableFuture;

public class MainActivity extends FragmentActivity {
    private BoardFragment mainBoard;
    private ObjectBoxSettingManager settingManager;
    private TextView xCoordDisplay;
    private TextView yCoordDisplay;
    private LinearLayout addButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Window w = getWindow();
        w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Inject variables
        settingManager = ObjectBoxSettingManager.get();

        setContentView(R.layout.activity_main);

        // Find elements
        xCoordDisplay = findViewById(R.id.x_position);
        yCoordDisplay = findViewById(R.id.y_position);
        addButton = findViewById(R.id.addButton);

        // Initialization UI
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        DeviceProperties.setScreenSize(displayMetrics.widthPixels, displayMetrics.heightPixels);

        initializeBoard(settingManager.getLastVisitedBoard());

        // Set properties
        LinearLayout boardInfo = findViewById(R.id.board_info);
        boardInfo.setOnTouchListener((view, motionEvent) -> {return true;});

        addButton.setOnClickListener(this::addButtonOnClick);

        LinearLayout boardIndicator = findViewById(R.id.board_indicator);
        boardIndicator.setOnClickListener(view -> {
            // Close any dialog fragment
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
            if (prev != null) {
                ft.remove(prev);
            }

            // Show edit fragment
            BoardEditDialogFragment fragment = new BoardEditDialogFragment(mainBoard.getBoard());
            ft.addToBackStack(null);
            fragment.show(ft, "dialog");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        View decorView = getWindow().getDecorView();
        // Hide both the navigation bar and the status bar.
        // SYSTEM_UI_FLAG_FULLSCREEN is only available on Android 4.1 and higher, but as
        // a general rule, you should design your app to hide the status bar whenever you
        // hide the navigation bar.
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
    }

    public void initializeBoard(long boardId) {
        if (mainBoard != null) {
            ((ConstraintLayout) findViewById(R.id.app_view)).removeView(mainBoard);
        }

        mainBoard = new BoardFragment(this, boardId);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        ((ConstraintLayout) findViewById(R.id.app_view)).addView(mainBoard, lp);

        // Keep this layer order for shadow
        LinearLayout boardInfo = findViewById(R.id.board_info);
        boardInfo.bringToFront();
    }

    public void moveBoardTo(Point2D position) {
        Point2D currentCoord = mainBoard.moveTo(position);
        setCoordDisplay((int) -currentCoord.getX(), (int) currentCoord.getY());
    }

    public void setBoardInfo(Board board) {
        ((ImageView) findViewById(R.id.board_color)).setImageResource(board.color.getRoundId());
        ((TextView) findViewById(R.id.board_name)).setText(board.name);
    }

    /**
     * Update coordinate display on UI
     *
     * @param x x position
     * @param y y position
     */
    public void setCoordDisplay(int x, int y) {
        xCoordDisplay.setText(Integer.toString(x));
        yCoordDisplay.setText(Integer.toString(y));
    }

    public void deleteAllNotes() {
        ObjectBox.get().removeAllObjects();
    }

    public void addButtonOnClick(View view) {
        // Close any dialog fragment
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }

        // Show note type select dialog fragment
        SelectNoteTypeDialogFragment fragment = new SelectNoteTypeDialogFragment(type -> {
            CompletableFuture<Note> dbAddFuture = CompletableFuture.supplyAsync(() -> mainBoard.addToDatabase(type));

            dbAddFuture.handle((newNote, throwable) -> {
                if (throwable != null) {
                    Log.d(this.getLocalClassName(), "Failed to add new note.");
                    throwable.printStackTrace();
                }

                return newNote;
            }).thenAccept(newNote -> runOnUiThread(() -> mainBoard.renderNote(newNote, true)));
        });
        ft.addToBackStack(null);
        fragment.show(ft, "dialog");
    }
}