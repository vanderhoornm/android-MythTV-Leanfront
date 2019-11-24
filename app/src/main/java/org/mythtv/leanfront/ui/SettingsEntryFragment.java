package org.mythtv.leanfront.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;

import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;
import androidx.preference.PreferenceManager;

import org.mythtv.leanfront.R;

import java.util.ArrayList;
import java.util.List;

public class SettingsEntryFragment extends GuidedStepSupportFragment {

    private static final int ID_BACKEND = 1;
    private static final int ID_HTTP_PORT = 2;
    private static final int ID_BOOKMARK_STRATEGY = 3;
    private static final int ID_BOOKMARK_MYTHTV = 4;
    private static final int ID_BOOKMARK_LOCAL = 5;
    private static final int ID_BOOKMARK_FPS = 6;
    private static final int ID_PLAYBACK = 7;
    private static final int ID_SKIP_FWD = 8;
    private static final int ID_SKIP_BACK = 9;
    private static final int ID_JUMP = 10;

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEditor;

    private GuidedAction mBookmarkAction;

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        Activity activity = getActivity();
        String title = "Settings";
        String breadcrumb = "";
        String description = getString(R.string.pref_title_settings);
        Drawable icon = activity.getDrawable(R.drawable.ic_settings);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences (getActivity());
        mEditor = mPrefs.edit();

        actions.add(new GuidedAction.Builder(getActivity())
                .id(ID_BACKEND)
                .title(R.string.pref_title_backend)
                .description(mPrefs.getString("pref_backend", null))
                .descriptionEditable(true)
                .build());
        actions.add(new GuidedAction.Builder(getActivity())
                .id(ID_HTTP_PORT)
                .title(R.string.pref_title_http_port)
                .description(mPrefs.getString("pref_http_port", "6544"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        
        List<GuidedAction> subActions = new ArrayList<GuidedAction>();
        String bookmark = mPrefs.getString("pref_bookmark", "mythtv");
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_BOOKMARK_MYTHTV)
                .title(R.string.pref_bookmark_mythtv)
                .checkSetId(ID_BOOKMARK_STRATEGY)
                .checked("mythtv".equals(bookmark))
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_BOOKMARK_LOCAL)
                .title(R.string.pref_bookmark_local)
                .checkSetId(ID_BOOKMARK_STRATEGY)
                .checked("local".equals(bookmark))
//                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_BOOKMARK_FPS)
                .title(R.string.pref_title_fps)
                .description(mPrefs.getString("pref_fps", "30"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        actions.add(mBookmarkAction = new GuidedAction.Builder(getActivity())
                .id(ID_BOOKMARK_STRATEGY)
                .title(R.string.pref_title_bookmark_strategy)
                .description(bookmarkDesc(bookmark))
                .subActions(subActions)
                .build());

        subActions = new ArrayList<GuidedAction>();
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_SKIP_FWD)
                .title(R.string.pref_title_skip_fwd)
                .description(mPrefs.getString("pref_skip_fwd", "60"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_SKIP_BACK)
                .title(R.string.pref_title_skip_back)
                .description(mPrefs.getString("pref_skip_back", "20"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_JUMP)
                .title(R.string.pref_title_jump)
                .description(mPrefs.getString("pref_jump", "5"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        actions.add(new GuidedAction.Builder(getActivity())
                .id(ID_PLAYBACK)
                .title(R.string.pref_title_playback)
                .subActions(subActions)
                .build());

    }

    private int bookmarkDesc(String bookmark) {
        if ("mythtv".equals(bookmark))
            return R.string.pref_bookmark_mythtv;
        if ("local".equals(bookmark))
            return R.string.pref_bookmark_local;
        return R.string.dummy_empty_string;
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        super.onGuidedActionClicked(action);
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        switch((int) action.getId()) {
            case ID_BACKEND:
                mEditor.putString("pref_backend",action.getDescription().toString());
                break;
            case ID_HTTP_PORT:
                mEditor.putString("pref_http_port",action.getDescription().toString());
                break;
            case ID_BOOKMARK_FPS:
                mEditor.putString("pref_fps",action.getDescription().toString());
                break;
            case ID_SKIP_FWD:
                mEditor.putString("pref_skip_fwd",action.getDescription().toString());
                break;
            case ID_SKIP_BACK:
                mEditor.putString("pref_skip_back",action.getDescription().toString());
                break;
            case ID_JUMP:
                mEditor.putString("pref_jump",action.getDescription().toString());
                break;
            default:
                return GuidedAction.ACTION_ID_CURRENT;
        }
        mEditor.apply();
        return GuidedAction.ACTION_ID_CURRENT;
    }

    @Override
    public void onGuidedActionEditCanceled(GuidedAction action) {
        switch((int) action.getId()) {
            case ID_BACKEND:
                action.setDescription(mPrefs.getString("pref_backend", null));
                break;
            case ID_HTTP_PORT:
                action.setDescription(mPrefs.getString("pref_http_port", "6544"));
                break;
            case ID_BOOKMARK_FPS:
                action.setDescription(mPrefs.getString("pref_fps", "30"));
                break;
            case ID_SKIP_FWD:
                action.setDescription(mPrefs.getString("pref_skip_fwd", "60"));
                break;
            case ID_SKIP_BACK:
                action.setDescription(mPrefs.getString("pref_skip_back", "20"));
                break;
            case ID_JUMP:
                action.setDescription(mPrefs.getString("pref_jump", "5"));
                break;
        }
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {
        if (!action.isChecked())
            return false;
        switch((int) action.getId()) {
            case ID_BOOKMARK_MYTHTV:
                mEditor.putString("pref_bookmark", "mythtv");
                break;
            case ID_BOOKMARK_LOCAL:
                mEditor.putString("pref_bookmark", "local");
                break;
            default:
                return false;
        }
        mBookmarkAction.setDescription(action.getTitle());
        notifyActionChanged(findActionPositionById(ID_BOOKMARK_STRATEGY));
        mEditor.apply();
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity.getContext().getMainFragment().startFetch();
    }
}
