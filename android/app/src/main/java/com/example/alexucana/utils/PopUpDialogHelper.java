package com.example.alexucana.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.alexucana.R;

/**
 * The PopUpDialogHelper class provides utility methods for displaying popup dialogs with different states.
 */
public class PopUpDialogHelper {
    /**
     * Dialog types indicating the state of the operation.
     */
    public static final int TYPE_PENDING = 0;
    public static final int TYPE_SUCCESS = 1;
    public static final int TYPE_FAILED = 2;

    // Reference to the currently active dialog
    private static AlertDialog dialog = null;

    /**
     * Shows a popup dialog based on the specified type, with the given message.
     *
     * @param context    The context in which the dialog should be displayed.
     * @param TYPE       The type of the dialog (pending, success, or failure).
     * @param cancelable Whether the dialog is cancelable or not.
     * @param message    The message to be displayed in the dialog.
     */
    public static void showDialog(Context context, int TYPE, boolean cancelable, String message) {
        // Dismiss any existing dialog
        if (dialog != null) {
            dismissDialog();
        }

        // Log the package name for debugging
        Log.d("dialog", context.getPackageName());

        // Create a layout for the dialog based on the specified type
        final LinearLayout ll;
        switch (TYPE) {
            case TYPE_PENDING:
                ll = makePending(context, message);
                break;
            case TYPE_SUCCESS:
                ll = makeSuccess(context, message);
                break;
            case TYPE_FAILED:
                ll = makeFailure(context, message);
                break;
            default:
                throw new RuntimeException("Unknown type of dialog");
        }

        // Build the AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(cancelable);
        builder.setView(ll);

        // Create and show the dialog
        dialog = builder.create();
        dialog.show();

        // Set dialog window attributes
        Window window = dialog.getWindow();
        Log.d("window", window.getContext().getPackageName());
        if (window == null) return;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT;
        layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT;
        dialog.getWindow().setAttributes(layoutParams);
    }

    // Helper method to create a container layout for dialog elements
    private static LinearLayout makeContainer(Context context) {
        final LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setPadding(30, 30, 30, 30);
        ll.setGravity(Gravity.CENTER);
        final LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        ll.setLayoutParams(llParam);
        return ll;
    }

    // Helper method to create a TextView for displaying text in the dialog
    private static TextView makeText(Context context, LinearLayout.LayoutParams llParam, String message) {
        final TextView tvText = new TextView(context);
        tvText.setText(message);
        tvText.setTextColor(Color.parseColor("#000000"));
        tvText.setTextSize(20);
        tvText.setPadding(30, 0, 0, 0);
        tvText.setLayoutParams(llParam);
        return tvText;
    }

    // Helper method to create a ProgressBar for a loading indicator in the dialog
    private static ProgressBar makeLoadingIndicator(Context context, LinearLayout.LayoutParams llParam) {
        final ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        progressBar.setLayoutParams(llParam);
        return progressBar;
    }

    // Helper method to create an ImageView for displaying an icon in the dialog
    private static ImageView makeIcon(Context context, int resId, LinearLayout.LayoutParams llParam) {
        final ImageView vIcon = new ImageView(context);
        vIcon.setImageResource(resId);
        vIcon.setLayoutParams(llParam);
        return vIcon;
    }

    // Helper method to create a dialog layout for the pending state
    @NonNull
    private static LinearLayout makePending(Context context, String message) {
        final LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        final LinearLayout ll = makeContainer(context);

        ll.addView(makeLoadingIndicator(context, llParam));
        ll.addView(makeText(context, llParam, message));
        return ll;
    }

    // Helper method to create a dialog layout for the failure state
    @NonNull
    private static LinearLayout makeFailure(Context context, String message) {
        final LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        final LinearLayout ll = makeContainer(context);
        ll.addView(makeIcon(context, R.drawable.xmark, llParam));
        ll.addView(makeText(context, llParam, message));
        return ll;
    }

    // Helper method to create a dialog layout for the success state
    @NonNull
    private static LinearLayout makeSuccess(Context context, String message) {
        final LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        final LinearLayout ll = makeContainer(context);
        ll.addView(makeIcon(context, R.drawable.checkmark, llParam));
        ll.addView(makeText(context, llParam, message));
        return ll;
    }

    // Helper method to dismiss the currently active dialog
    private static void dismissDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }
}
