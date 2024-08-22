package org.sfa.volunteer.model;

import lombok.Getter;
import org.joda.time.DateTime;

import java.util.List;

@Getter
public class Volunteer {
    private int id;
    private String name;
    private Skill skill;
    private List<DateTime> availability;
    private String preferredLocation;
    private AssistanceMode assistanceMode;

    private enum Skill {
        NURSE
    }
}

