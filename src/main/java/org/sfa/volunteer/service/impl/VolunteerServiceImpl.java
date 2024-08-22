package org.sfa.volunteer.service.impl;

import org.sfa.volunteer.model.Volunteer;
import org.sfa.volunteer.model.VolunteerRequest;
import org.sfa.volunteer.service.VolunteerService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class VolunteerServiceImpl implements VolunteerService {
    @Override
    public boolean addVolunteerRequest(VolunteerRequest volunteerRequest) {
        return false;
    }

    @Override
    public List<Volunteer> matchVolunteers(VolunteerRequest volunteerRequest) {
        List<Volunteer> volunteers = new ArrayList<>();
        volunteers = volunteers.stream()
                .filter(volunteer -> volunteer.getAssistanceMode().name().equals(volunteerRequest.getAssistanceMode().name()))
                .collect(Collectors.toCollection(ArrayList::new));






    }

    @Override
    public boolean addVolunteer(Volunteer volunteer) {
        return false;
    }

    private static double cosineSimilarity(double[] vectorA, double[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
