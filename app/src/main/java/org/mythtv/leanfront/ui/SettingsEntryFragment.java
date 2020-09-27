/*
 * Copyright (c) 2019-2020 Peter Bennett
 *
 * This file is part of MythTV-leanfront.
 *
 * MythTV-leanfront is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * MythTV-leanfront is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with MythTV-leanfront.  If not, see <https://www.gnu.org/licenses/>.
 */

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

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.model.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SettingsEntryFragment extends GuidedStepSupportFragment {

    private static final int ID_BACKEND_IP = 1;
    private static final int ID_HTTP_PORT = 2;
    private static final int ID_BOOKMARK_LOCAL = 5;
    private static final int ID_PLAYBACK = 7;
    private static final int ID_SKIP_FWD = 8;
    private static final int ID_SKIP_BACK = 9;
    private static final int ID_JUMP = 10;
    private static final int ID_SORT = 11;
    private static final int ID_SORT_ORIG_AIRDATE = 12;
    private static final int ID_DESCENDING = 13;
    private static final int ID_BACKEND_MAC = 14;
    private static final int ID_BACKEND = 15;
    private static final int ID_AUDIO = 16;
    private static final int ID_AUDIO_AUTO = 17;
    private static final int ID_AUDIO_MEDIACODEC = 18;
    private static final int ID_AUDIO_FFMPEG = 19;
    private static final int ID_ARROW_JUMP = 20;
    private static final int ID_LIVETV_DURATION = 21;
    private static final int ID_FRAMERATE_MATCH = 22;
    private static final int ID_SUBTITLE_SIZE = 23;

    private SharedPreferences.Editor mEditor;

    private GuidedAction mBackendAction;
    private GuidedAction mSortAction;
    private GuidedAction mAudioAction;

    private String mPriorBackend;
    private String mPriorHttpPort;

    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        Activity activity = getActivity();
        String title = getString(R.string.personal_settings);
        String breadcrumb = "";
        String description = getString(R.string.pref_title_settings);
        Drawable icon = activity.getDrawable(R.drawable.ic_settings);
        return new GuidanceStylist.Guidance(title, description, breadcrumb, icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> actions, Bundle savedInstanceState) {
        mEditor = Settings.getEditor();

        List<GuidedAction> subActions = new ArrayList<>();
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_BACKEND_IP)
                .title(R.string.pref_title_backend_ip)
                .description(Settings.getString("pref_backend"))
                .descriptionEditable(true)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_HTTP_PORT)
                .title(R.string.pref_title_http_port)
                .description(Settings.getString("pref_http_port"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_BACKEND_MAC)
                .title(R.string.pref_title_backend_mac)
                .description(Settings.getString("pref_backend_mac"))
                .descriptionEditable(true)
                .build());
        actions.add(mBackendAction = new GuidedAction.Builder(getActivity())
                .id(ID_BACKEND)
                .title(R.string.pref_title_backend)
                .description(backendDesc())
                .subActions(subActions)
                .build());

        subActions = new ArrayList<>();
        String match = Settings.getString("pref_framerate_match");
        GuidedAction.Builder tmp = new GuidedAction.Builder(getActivity());
        tmp     .id(ID_FRAMERATE_MATCH)
                .title(R.string.pref_title_framerate_match)
                .checked("true".equals(match))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID);
        if (android.os.Build.VERSION.SDK_INT < 23)
            tmp .description(R.string.pref_msg_needs_6_0)
                .enabled(false);
        subActions.add(tmp.build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_SKIP_FWD)
                .title(R.string.pref_title_skip_fwd)
                .description(Settings.getString("pref_skip_fwd"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_SKIP_BACK)
                .title(R.string.pref_title_skip_back)
                .description(Settings.getString("pref_skip_back"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        String arrowjump = Settings.getString("pref_arrow_jump");
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_ARROW_JUMP)
                .title(R.string.pref_title_arrow_jump)
                .checked("true".equals(arrowjump))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_JUMP)
                .title(R.string.pref_title_jump)
                .description(Settings.getString("pref_jump"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_LIVETV_DURATION)
                .title(R.string.pref_title_livetv_duration)
                .description(Settings.getString("pref_livetv_duration"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        String bookmark = Settings.getString("pref_bookmark");
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_BOOKMARK_LOCAL)
                .title(R.string.pref_bookmark_local)
                .checked("local".equals(bookmark))
                .description(R.string.pref_bookmark_mythtv)
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_SUBTITLE_SIZE)
                .title(R.string.pref_title_subtitle_size)
                .description(Settings.getString("pref_subtitle_size"))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build());
        actions.add(new GuidedAction.Builder(getActivity())
                .id(ID_PLAYBACK)
                .title(R.string.pref_title_playback)
                .subActions(subActions)
                .build());

        subActions = new ArrayList<>();
        String seq = Settings.getString("pref_seq");
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_SORT_ORIG_AIRDATE)
                .title(R.string.pref_seq_orig_airdate)
                .checked("airdate".equals(seq))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .build());
        String ascdesc = Settings.getString("pref_seq_ascdesc");
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_DESCENDING)
                .title(R.string.pref_seq_descending)
                .checked("desc".equals(ascdesc))
                .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                .build());
        actions.add(mSortAction = new GuidedAction.Builder(getActivity())
                .id(ID_SORT)
                .title(R.string.pref_sort_order)
                .description(sortDesc())
                .subActions(subActions)
                .build());

        subActions = new ArrayList<GuidedAction>();

        String audio = Settings.getString("pref_audio");
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_AUDIO_AUTO)
                .title(R.string.pref_audio_auto)
                .checked("auto".equals(audio))
                .checkSetId(ID_AUDIO)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_AUDIO_MEDIACODEC)
                .title(R.string.pref_audio_mediacodec)
                .checked("mediacodec".equals(audio))
                .checkSetId(ID_AUDIO)
                .build());
        subActions.add(new GuidedAction.Builder(getActivity())
                .id(ID_AUDIO_FFMPEG)
                .title(R.string.pref_audio_ffmpeg)
                .checked("ffmpeg".equals(audio))
                .checkSetId(ID_AUDIO)
                .build());
        actions.add(mAudioAction = new GuidedAction.Builder(getActivity())
                .id(ID_AUDIO)
                .title(R.string.pref_ff_title)
                .description(audiodesc())
                .subActions(subActions)
                .build());
    }

    private String backendDesc() {
        return Settings.getString("pref_backend");
    }

    private String sortDesc() {
        StringBuilder builder = new StringBuilder();
        String seq = Settings.getString("pref_seq");
        String ascdesc = Settings.getString("pref_seq_ascdesc");
        if ("airdate".equals(seq))
            builder.append(getActivity().getString(R.string.pref_seq_orig_airdate));
        else
            builder.append(getActivity().getString(R.string.pref_seq_rec_time));
        builder.append(" ");
        if ("desc".equals(ascdesc))
            builder.append(getActivity().getString(R.string.pref_seq_descending));
        else
            builder.append(getActivity().getString(R.string.pref_seq_ascending));
        return builder.toString();
    }

    private String audiodesc() {
        return Settings.getString("pref_audio");
    }

    @Override
    public void onGuidedActionClicked(GuidedAction action) {
        super.onGuidedActionClicked(action);
    }

    @Override
    public long onGuidedActionEditedAndProceed(GuidedAction action) {
        switch((int) action.getId()) {
            case ID_BACKEND_IP:
                String newVal = action.getDescription().toString();
                mEditor.putString("pref_backend",newVal);
                mBackendAction.setDescription(newVal);
                notifyActionChanged(findActionPositionById(ID_BACKEND));
                break;
            case ID_HTTP_PORT:
                mEditor.putString("pref_http_port",
                    validateNumber(action, 1, 65535, 6544));
                break;
            case ID_BACKEND_MAC:
                mEditor.putString("pref_backend_mac",action.getDescription().toString());
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
            case ID_LIVETV_DURATION:
                mEditor.putString("pref_livetv_duration",
                        validateNumber(action, 30, 240, 60));
                break;
            case ID_SUBTITLE_SIZE:
                mEditor.putString("pref_subtitle_size",
                        validateNumber(action, 25, 300, 100));
                break;
            default:
                return GuidedAction.ACTION_ID_CURRENT;
        }
        mEditor.apply();
        return GuidedAction.ACTION_ID_CURRENT;
    }

    private static String validateNumber(GuidedAction action, int min, int max, int defValue) {
        String s;
        int i;
        s = action.getDescription().toString();
        try {
            i = Integer.parseInt(s);
        } catch (Exception e) {
            i = defValue;
        }
        if (i < min)
            i = min;
        else if (i > max)
            i = max;
        s = String.valueOf(i);
        action.setDescription(s);
        return s;
    }

    @Override
    public void onGuidedActionEditCanceled(GuidedAction action) {
        switch((int) action.getId()) {
            case ID_BACKEND_IP:
                action.setDescription(Settings.getString("pref_backend"));
                break;
            case ID_BACKEND_MAC:
                action.setDescription(Settings.getString("pref_backend_mac"));
                break;
            case ID_HTTP_PORT:
                action.setDescription(Settings.getString("pref_http_port"));
                break;
            case ID_SKIP_FWD:
                action.setDescription(Settings.getString("pref_skip_fwd"));
                break;
            case ID_SKIP_BACK:
                action.setDescription(Settings.getString("pref_skip_back"));
                break;
            case ID_JUMP:
                action.setDescription(Settings.getString("pref_jump"));
                break;
            case ID_LIVETV_DURATION:
                action.setDescription(Settings.getString("pref_livetv_duration"));
                break;
            case ID_SUBTITLE_SIZE:
                action.setDescription(Settings.getString("pref_subtitle_size"));
                break;
        }
    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {

        int id = (int) action.getId();
        switch(id) {
            case ID_BOOKMARK_LOCAL:
                if (action.isChecked())
                    mEditor.putString("pref_bookmark", "local");
                else
                    mEditor.putString("pref_bookmark", "mythtv");
                break;
            case ID_FRAMERATE_MATCH:
                if (action.isChecked())
                    mEditor.putString("pref_framerate_match", "true");
                else
                    mEditor.putString("pref_framerate_match", "false");
                break;
            case ID_ARROW_JUMP:
                if (action.isChecked())
                    mEditor.putString("pref_arrow_jump", "true");
                else
                    mEditor.putString("pref_arrow_jump", "false");
                break;
            case ID_SORT_ORIG_AIRDATE:
                if (action.isChecked())
                    mEditor.putString("pref_seq", "airdate");
                else
                    mEditor.putString("pref_seq", "rectime");
                break;
            case ID_DESCENDING:
                if (action.isChecked())
                    mEditor.putString("pref_seq_ascdesc", "desc");
                else
                    mEditor.putString("pref_seq_ascdesc", "asc");
                break;
            case ID_AUDIO_AUTO:
                if (action.isChecked())
                    mEditor.putString("pref_audio", "auto");
                break;
            case ID_AUDIO_MEDIACODEC:
                if (action.isChecked())
                    mEditor.putString("pref_audio", "mediacodec");
                break;
            case ID_AUDIO_FFMPEG:
                if (action.isChecked())
                    mEditor.putString("pref_audio", "ffmpeg");
                break;
            default:
                return false;
        }
        mEditor.apply();
        notifyActionChanged(findActionPositionById(ID_PLAYBACK));
        mSortAction.setDescription(sortDesc());
        notifyActionChanged(findActionPositionById(ID_SORT));
        mAudioAction.setDescription(audiodesc());
        notifyActionChanged(findActionPositionById(ID_AUDIO));
        return false;
    }

    @Override
    public void onResume() {
        mPriorBackend = Settings.getString("pref_backend");
        mPriorHttpPort =  Settings.getString("pref_http_port");
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!Objects.equals(mPriorBackend, Settings.getString("pref_backend"))
          || !Objects.equals(mPriorHttpPort, Settings.getString("pref_http_port")))
            MainActivity.getContext().getMainFragment().startFetch(-1, null, null);
        mPriorBackend = null;
        mPriorHttpPort = null;
    }
}
