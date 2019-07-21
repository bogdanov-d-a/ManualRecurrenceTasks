package utils;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import android.text.format.DateFormat;

public abstract class TimePickerHelper extends DialogFragment {
    public static void createAndShow(TimePickerHelper fragment, FragmentManager manager, int hourOfDay, int minute) {
        final Bundle bundle = new Bundle();
        bundle.putInt(Utils.HOUR_TAG, hourOfDay);
        bundle.putInt(Utils.MINUTE_TAG, minute);
        fragment.setArguments(bundle);

        fragment.show(manager, "TimePickerFragment");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        final Bundle bundle = getArguments();

        return new TimePickerDialog(
                getActivity(),
                getOnTimeSetListener(),
                bundle.getInt(Utils.HOUR_TAG),
                bundle.getInt(Utils.MINUTE_TAG),
                DateFormat.is24HourFormat(getActivity())
        );
    }

    public abstract TimePickerDialog.OnTimeSetListener getOnTimeSetListener();
}
