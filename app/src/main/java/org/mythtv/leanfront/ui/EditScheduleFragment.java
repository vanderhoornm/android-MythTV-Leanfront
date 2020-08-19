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
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.leanback.app.GuidedStepSupportFragment;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import org.mythtv.leanfront.R;
import org.mythtv.leanfront.data.XmlNode;
import org.mythtv.leanfront.model.RecordRule;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class EditScheduleFragment extends GuidedStepSupportFragment {

    private RecordRule mProgDetails;
    private RecordRule mRecordRule;
    private ArrayList<XmlNode> mDetailsList;
    private ArrayList<RecordRule> mTemplateList = new ArrayList<>();
    private ArrayList<String> mPlayGroupList;
    private ArrayList<String> mRecGroupList;
    private ArrayList<String> mRecStorageGroupList;
    private SparseArray<String> mInputList = new SparseArray<>();
    private ArrayList<String> mRecRuleFilterList;
    private int mGroupId;
    private List<GuidedAction> mMainActions;
    private ArrayList<ActionGroup> mGroupList = new ArrayList<>();

    private static DateFormat timeFormatter;
    private static DateFormat dateFormatter;
    private static DateFormat dayFormatter;

    private static final int ACTIONTYPE_RADIOBNS = 1;
    private static final int ACTIONTYPE_CHECKBOXES = 2;
    private static final int ACTIONTYPE_NUMERIC = 3;
    private static final int ACTIONTYPE_TEXT = 4;


    public EditScheduleFragment(ArrayList<XmlNode> detailsList) {

        /*
            Details are in this order
                Video.ACTION_GETPROGRAMDETAILS,
                Video.ACTION_GETRECORDSCHEDULELIST,
                Video.ACTION_GETPLAYGROUPLIST,
                Video.ACTION_GETRECGROUPLIST,
                Video.ACTION_GETRECSTORAGEGROUPLIST,
                Video.ACTION_GETINPUTLIST
                Video.ACTION_GETRECRULEFILTERLIST
         */
        mDetailsList = detailsList;
    }

    private void setupData() {
        RecordRule defaultTemplate = null;
        XmlNode progDetailsNode = mDetailsList.get(0); // ACTION_GETPROGRAMDETAILS
        if (progDetailsNode != null)
            mProgDetails = new RecordRule().fromProgram(progDetailsNode);
        XmlNode recRulesNode = mDetailsList.get(1) // ACTION_GETRECORDSCHEDULELIST
                .getNode("RecRules");
        if (recRulesNode != null) {
            XmlNode recRuleNode = recRulesNode.getNode("RecRule");
            while (recRuleNode != null) {
                int id = Integer.parseInt(recRuleNode.getString("Id"));
                if (id == mProgDetails.recordId)
                    mRecordRule = new RecordRule().fromSchedule(recRuleNode);
                String type = recRuleNode.getString("Type");
                if ("Recording Template".equals(type)) {
                    RecordRule template = new RecordRule().fromSchedule(recRuleNode);
                    mTemplateList.add(template);
                    if ("Default (Template)".equals(template.title))
                        defaultTemplate = template;
                }
                recRuleNode = recRuleNode.getNextSibling();
            }
        }
        if (mRecordRule == null)
            mRecordRule = new RecordRule().mergeTemplate(defaultTemplate);
        if (mProgDetails != null)
            mRecordRule.mergeProgram(mProgDetails);

        // Lists
        mPlayGroupList = getStringList(mDetailsList.get(2)); // ACTION_GETPLAYGROUPLIST
        mRecGroupList = getStringList(mDetailsList.get(3)); // ACTION_GETRECGROUPLIST
        mRecStorageGroupList = getStringList(mDetailsList.get(4)); // ACTION_GETRECSTORAGEGROUPLIST

        mInputList.put(0, getContext().getString(R.string.sched_input_any));
        XmlNode inputListNode = mDetailsList.get(5); // ACTION_GETINPUTLIST
        if (inputListNode != null) {
            XmlNode inputsNode = inputListNode.getNode("Inputs");
            if (inputsNode != null) {
                XmlNode inputNode = inputsNode.getNode("Input");
                while (inputNode != null) {
                    int id = inputNode.getNode("Id").getInt(-1);
                    String displayName = inputNode.getString("DisplayName");
                    if (id > 0)
                        mInputList.put(id, displayName);
                    inputNode = inputNode.getNextSibling();
                }
            }
        }
        mRecRuleFilterList = new ArrayList<>();
        XmlNode filterListNode = mDetailsList.get(6); // ACTION_GETRECRULEFILTERLIST
        if (filterListNode != null) {
            XmlNode filtersNode = filterListNode.getNode("RecRuleFilters");
            if (filtersNode != null) {
                XmlNode filterNode = filtersNode.getNode("RecRuleFilter");
                while (filterNode != null) {
                    int id = filterNode.getNode("Id").getInt(-1);
                    String description = filterNode.getString("Description");
                    for (int ix = mRecRuleFilterList.size(); ix <= id; ix++)
                        mRecRuleFilterList.add(null);
                    if (id > 0)
                        mRecRuleFilterList.set(id, description);
                    filterNode = filterNode.getNextSibling();
                }
            }
        }
    }


    public static ArrayList<String> getStringList(XmlNode listNode) {
        ArrayList<String> ret = new ArrayList<>();
        ret.add("Default");
        if (listNode != null) {
            XmlNode stringNode = listNode.getNode("String");
            while (stringNode != null) {
                String value = stringNode.getString();
                if (!"Default".equals(value))
                    ret.add(value);
                stringNode = stringNode.getNextSibling();
            }
        }
        return ret;
    }


    @Override
    public GuidanceStylist.Guidance onCreateGuidance(Bundle savedInstanceState) {
        if (timeFormatter == null) {
            timeFormatter = android.text.format.DateFormat.getTimeFormat(getContext());
            dateFormatter = android.text.format.DateFormat.getLongDateFormat(getContext());
            dayFormatter = new SimpleDateFormat("EEE ");
        }
        setupData();
        Activity activity = getActivity();
        String title = mRecordRule.title;
        StringBuilder dateTime = new StringBuilder();
        dateTime.append(mRecordRule.station).append(' ')
                .append(dayFormatter.format(mRecordRule.startTime))
                .append(dateFormatter.format(mRecordRule.startTime)).append(' ')
                .append(timeFormatter.format(mRecordRule.startTime));
        StringBuilder details = new StringBuilder();
        String subTitle = mRecordRule.subtitle;
        if (subTitle != null)
            details.append(subTitle).append("\n");
        String desc = mRecordRule.description;
        if (desc != null)
            details.append(desc);
        Drawable icon = activity.getDrawable(R.drawable.ic_voicemail);
        return new GuidanceStylist.Guidance(title, details.toString(), dateTime.toString(), icon);
    }

    @Override
    public void onCreateActions(@NonNull List<GuidedAction> mainActions, Bundle savedInstanceState) {
        mMainActions = mainActions;
        int[] prompts = {
                R.string.sched_type_not, R.string.sched_type_this,
                R.string.sched_type_one, R.string.sched_type_all};
        String[] stringValues = {
                "Not Recording", "Single Record",
                "Record One", "Record All"};
        ActionGroup group = new ActionGroup(ACTIONTYPE_RADIOBNS, R.string.sched_type,
                prompts, stringValues,"Not Recording");
        mainActions.add(group.mGuidedAction);
        mGroupList.add(group);
        group = new ActionGroup(ACTIONTYPE_RADIOBNS, R.string.sched_type,
                prompts, stringValues,"Not Recording");


    }

    @Override
    public boolean onSubGuidedActionClicked(GuidedAction action) {
        int id = (int) action.getId();
        int group = id / 100;
        mGroupList.get(group).onSubGuidedActionClicked(action);
        return super.onSubGuidedActionClicked(action);
    }

    // Handles groups of checkboxes or radiobuttons.
    private class ActionGroup {
        int mActionType;
        int mTitle;
        int[] mPrompts;          // can be null in which case mStringValues are used
        int[] mIntValues;        // can be null
        String[] mStringValues;  // can be null
        int mId;                  // id of main action. sub actions have sequential ids after this.
        int mSubActionCount;
        int mIntResult = -1;
        String mStringResult;
        int mSelectedPrompt = -1;
        GuidedAction mGuidedAction;

        ActionGroup(int actionType, int title, int[] prompts, int[] intValues,
                    String[] stringValues, String currStringValue, int currIntValue) {
            mActionType = actionType;
            mIntValues = intValues;
            mStringValues = stringValues;
            mPrompts = prompts;
            mIntResult = currIntValue;
            mStringResult = currStringValue;
            mId = mGroupId++ * 100 + 1;
            int subId = mId;

            if (intValues != null)
                mSubActionCount = intValues.length;
            else
                mSubActionCount = stringValues.length;

            List<GuidedAction> subActions = new ArrayList<>();
            Context context = getContext();
            for (int ix = 0; ix < mSubActionCount; ix++) {
                GuidedAction.Builder builder = new GuidedAction.Builder(getActivity())
                        .id(++subId);
                if (mPrompts != null)
                    builder.title(mPrompts[ix]);
                else if (mStringValues != null)
                    builder.title(mStringValues[ix]);
                boolean checked = false;
                if (currStringValue != null)
                    checked = (currStringValue.equals(mStringValues[ix]));
                else
                    checked = (currIntValue == mIntValues[ix]);
                if (checked) {
                    if (mPrompts != null)
                        mSelectedPrompt = ix;
                    if (mStringValues != null)
                        mStringResult = mStringValues[ix];
                    if (mIntValues != null)
                        mIntResult = mIntValues[ix];
                }
                builder.checked(checked);
                switch (mActionType) {
                    case ACTIONTYPE_CHECKBOXES:
                        builder.checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID);
                        break;
                    case ACTIONTYPE_RADIOBNS:
                        builder.checkSetId(mId);
                        break;
                }
                subActions.add(builder.build());
            }
            GuidedAction.Builder builder = new GuidedAction.Builder(getActivity())
                    .id(mId)
                    .title(title);
            if (mActionType == ACTIONTYPE_RADIOBNS) {
                if (mSelectedPrompt >= 0)
                    builder.description(mPrompts[mSelectedPrompt]);
                else if (mStringResult != null)
                    builder.description(mStringResult);
            }
            builder.subActions(subActions);
            mGuidedAction = builder.build();
        }

        ActionGroup(int actionType, int title, @NonNull int[] prompts, @NonNull int[] intValues,
                    int currIntValue) {
            this(actionType, title, prompts, intValues,
                    null, null, currIntValue);
        }

        ActionGroup(int actionType, int title, int[] prompts,
                    @NonNull  String[] stringValues, String currStringValue) {
            this(actionType, title, prompts, null,
                    stringValues, currStringValue, -1);
        }

        public void onSubGuidedActionClicked(GuidedAction action) {
            int id = (int) action.getId();
            int ix = (id % 100) - 2;
            if (action.isChecked()) {
                if (mActionType == ACTIONTYPE_CHECKBOXES)
                    mIntResult |= (2 ^ ix);
                else if (mActionType == ACTIONTYPE_RADIOBNS) {
                    mSelectedPrompt = ix;
                    if (mIntValues != null)
                        mIntResult = mIntValues[ix];
                    if (mStringValues != null)
                        mStringResult = mStringValues[ix];
                    if (mPrompts != null) {
                        mGuidedAction.setDescription(getContext().getString(mPrompts[ix]));
                    } else if (mStringValues != null) {
                        mGuidedAction.setDescription(mStringResult);
                    }
                    notifyActionChanged(findActionPositionById(mId));
                }
            }
            else {
                if (mActionType == ACTIONTYPE_CHECKBOXES)
                    mIntResult &= (-1 - 2 ^ ix);
            }
        }
    }
}