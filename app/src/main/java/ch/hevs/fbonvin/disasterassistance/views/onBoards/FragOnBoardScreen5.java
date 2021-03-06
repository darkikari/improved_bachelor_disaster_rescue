package ch.hevs.fbonvin.disasterassistance.views.onBoards;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import ch.hevs.fbonvin.disasterassistance.R;
import ch.hevs.fbonvin.disasterassistance.utils.PreferencesManagement;

import static ch.hevs.fbonvin.disasterassistance.Constant.PREF_NOT_SET;

/**
 * A simple {@link Fragment} subclass.
 */
public class FragOnBoardScreen5 extends Fragment {

    public FragOnBoardScreen5() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_on_board_screen5, container, false);
        EditText editText = view.findViewById(R.id.et_on_board_username);


        String username = PreferencesManagement.
                getDefaultStringPref(
                        getActivity(),
                        getString(R.string.key_pref_user_name),
                        PREF_NOT_SET);

        if (!username.equals(PREF_NOT_SET)){
            editText.setText(username);
        }

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                ((ActivityOnBoard) getActivity()).onTextChange(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        return view;
    }
}
