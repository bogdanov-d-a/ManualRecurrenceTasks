package utils;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public abstract class DatePickerHelper extends DialogFragment {
    public static void createAndShow(DatePickerHelper fragment, FragmentManager manager, int year, int monthOfYear, int dayOfMonth) {
        final Bundle bundle = new Bundle();
        Utils.putDateToBundle(year, monthOfYear, dayOfMonth, bundle);
        fragment.setArguments(bundle);

        fragment.show(manager, "DatePickerFragment");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        final Bundle bundle = getArguments();

        return new DatePickerDialog(
                getActivity(),
                getOnDateSetListener(),
                bundle.getInt(Utils.YEAR_TAG),
                bundle.getInt(Utils.MONTH_TAG),
                bundle.getInt(Utils.DAY_TAG)
        );
    }

    public abstract DatePickerDialog.OnDateSetListener getOnDateSetListener();
}
