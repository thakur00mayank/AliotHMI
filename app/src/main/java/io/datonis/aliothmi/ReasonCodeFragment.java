package io.datonis.aliothmi;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by mayank on 15/4/17.
 */

public class ReasonCodeFragment extends Fragment {

    View reasonCodeView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        reasonCodeView = inflater.inflate(R.layout.reason_code_layout, container, false);
        return reasonCodeView;
    }
}
