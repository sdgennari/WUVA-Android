package com.poofstudios.android.wuvaradio.api.model;

import java.util.List;

public class RecordingResponse {

    public String created;
    public int count;
    public List<Recording> recordings;

    public class Recording {
        public String id;
        public String score;
        public String title;
        public List<Release> releases;

    }

    public class Release {
        public String id;
        public String title;
        public String status;
    }
}
