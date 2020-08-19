package org.mythtv.leanfront.model;

import org.mythtv.leanfront.data.XmlNode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

public class RecordRule {

    public int     recordId;
    public int     parentId;
    public String  title;
    public String  subtitle;
    public String  description;
    public String  category;
    public Date    startTime;
    public Date    endTime;
    public String  seriesId;
    public String  programId;
    public int     chanId;
    public String  station;
    public int     findDay = -1;
    public String  findTime;
    public boolean inactive;
    public int     season;
    public int     episode;
    public String  inetref;
    public String  type;
    public String  searchType;
    public int     recPriority;
    public int     preferredInput;
    public int     startOffset;
    public int     endOffset;
    public String  dupMethod;
    public String  dupIn;
    public int     filter;
    public String  recProfile;
    public String  recGroup;
    public String  storageGroup;
    public String  playGroup;
    public boolean autoExpire;
    public int     maxEpisodes;
    public boolean maxNewest;
    public boolean autoCommflag;
    public boolean autoTranscode;
    public boolean autoMetaLookup;
    public boolean autoUserJob1;
    public boolean autoUserJob2;
    public boolean autoUserJob3;
    public boolean autoUserJob4;
    public int     transcoder;

    private static final String TAG = "lfe";
    private static final String CLASS = "RecordSchedule";
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS");

    public RecordRule fromProgram(XmlNode programNode) {
        title = programNode.getString("Title");
        subtitle = programNode.getString("SubTitle");
        description = programNode.getString("Description");
        category = programNode.getString("Category");
        startTime = programNode.getNode("StartTime").getDate();
        endTime = programNode.getNode("EndTime").getDate();
        seriesId = programNode.getString("SeriesId");
        programId = programNode.getString("ProgramId");
        chanId = programNode.getNode("Channel").getNode("ChanId").getInt(0);
        station = programNode.getNode("Channel").getString("CallSign");
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(startTime);
        findDay = cal.get(GregorianCalendar.DAY_OF_WEEK);
        if (findDay == 7)
            findDay = 0;
        findTime = timeFormat.format(startTime);
        // inactive
        season = programNode.getNode("Season").getInt(0);
        episode = programNode.getNode("Episode").getInt(0);
        inetref = programNode.getString("Inetref");
        return this;
    }

    public RecordRule fromSchedule(XmlNode scheduleNode) {
        recordId = scheduleNode.getNode("Id").getInt(0);
        parentId = scheduleNode.getNode("ParentId").getInt(0);
        title = scheduleNode.getString("Title");
        subtitle = scheduleNode.getString("SubTitle");
        description = scheduleNode.getString("Description");
        category = scheduleNode.getString("Category");
        startTime = scheduleNode.getNode("StartTime").getDate();
        endTime = scheduleNode.getNode("EndTime").getDate();
        seriesId = scheduleNode.getString("SeriesId");
        programId = scheduleNode.getString("ProgramId");
        chanId = scheduleNode.getNode("ChanId").getInt(0);
        station = scheduleNode.getString("CallSign");
        findDay = scheduleNode.getNode("FindDay").getInt(0);
        findTime = scheduleNode.getString("FindTime");
        inactive = scheduleNode.getNode("Inactive").getBoolean();
        season = scheduleNode.getNode("Season").getInt(0);
        episode = scheduleNode.getNode("Episode").getInt(0);
        inetref = scheduleNode.getString("Inetref");
        type = scheduleNode.getString("Type");
        searchType = scheduleNode.getString("SearchType");
        recPriority = scheduleNode.getNode("RecPriority").getInt(0);
        preferredInput = scheduleNode.getNode("PreferredInput").getInt(0);
        startOffset = scheduleNode.getNode("StartOffset").getInt(0);
        endOffset = scheduleNode.getNode("EndOffset").getInt(0);
        dupMethod = scheduleNode.getString("DupMethod");
        dupIn = scheduleNode.getString("DupIn");
        filter = scheduleNode.getNode("Filter").getInt(0);
        recProfile = scheduleNode.getString("RecProfile");
        recGroup = scheduleNode.getString("RecGroup");
        storageGroup = scheduleNode.getString("StorageGroup");
        playGroup = scheduleNode.getString("PlayGroup");
        autoExpire = scheduleNode.getNode("AutoExpire").getBoolean();
        maxEpisodes = scheduleNode.getNode("MaxEpisodes").getInt(0);
        maxNewest = scheduleNode.getNode("MaxNewest").getBoolean();
        autoCommflag = scheduleNode.getNode("AutoCommflag").getBoolean();
        autoTranscode = scheduleNode.getNode("AutoTranscode").getBoolean();
        autoMetaLookup = scheduleNode.getNode("AutoMetaLookup").getBoolean();
        autoUserJob1 = scheduleNode.getNode("AutoUserJob1").getBoolean();
        autoUserJob2 = scheduleNode.getNode("AutoUserJob2").getBoolean();
        autoUserJob3 = scheduleNode.getNode("AutoUserJob3").getBoolean();
        autoUserJob4 = scheduleNode.getNode("AutoUserJob4").getBoolean();
        transcoder = scheduleNode.getNode("Transcoder").getInt(0);
        return this;
    }

    public RecordRule mergeProgram(RecordRule program) {
        if (program != null) {
            title = program.title;
            subtitle = program.subtitle;
            description = program.description;
            category = program.category;
            startTime = program.startTime;
            endTime = program.endTime;
            seriesId = program.seriesId;
            programId = program.programId;
            chanId = program.chanId;
            station = program.station;
            findDay = program.findDay;
            findTime = program.findTime;
            season = program.season;
            episode = program.episode;
            inetref = program.inetref;
        }
        return this;
    }

    public RecordRule mergeTemplate(RecordRule template) {
        if (template != null) {
            inactive = template.inactive;
            recPriority = template.recPriority;
            preferredInput = template.preferredInput;
            startOffset = template.startOffset;
            endOffset = template.endOffset;
            dupMethod = template.dupMethod;
            dupIn = template.dupIn;
            filter = template.filter;
            recProfile = template.recProfile;
            recGroup = template.recGroup;
            storageGroup = template.storageGroup;
            playGroup = template.playGroup;
            autoExpire = template.autoExpire;
            maxEpisodes = template.maxEpisodes;
            maxNewest = template.maxNewest;
            autoCommflag = template.autoCommflag;
            autoTranscode = template.autoTranscode;
            autoMetaLookup = template.autoMetaLookup;
            autoUserJob1 = template.autoUserJob1;
            autoUserJob2 = template.autoUserJob2;
            autoUserJob3 = template.autoUserJob3;
            autoUserJob4 = template.autoUserJob4;
            transcoder = template.transcoder;
        }
        return this;
    }

}
