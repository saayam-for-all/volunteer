package com.volunteer.microservice.VolunteerService.Exceptions;

public class ResourceNotFoundException extends RuntimeException{
    public ResourceNotFoundException(){

        // any other properties can be added also......
        super("Resource not found on server !!");
    }
    public ResourceNotFoundException(String Message){
        super(Message);
    }
}
