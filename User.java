package com.restapi.crud.firstcrud.Model;

import jakarta.persistence.*;
import java.util.*;

@Entity
public class User {

    @Id
    private long user_id;

    @Id
    private long user_type_id;

    @Column
    private char First_Name;

    @Column
    private char Middle_Name;

    @Column
    private char Last_Name;

    @Column
    private char Email_id;

    @Column
    private long phone_number;

    @Column
    private char Mobile_number;

    @Column
    private char Address_Ln1;

    @Column
    private char Address_Ln2;

    @Column
    private char Address_Ln3;

    @Column
    private long city;

    @Column
    private char country;

    @Column
    private long zipcode;

    @Column
    private Date Registered_date;

    @Column
    private long user_status_id;

    @Column
    private Date user_status_date;

    @Column
    private char emergency_availability_ind;

    @Override
    public String toString() {
        return "User{" +
                "Registered_date=" + Registered_date +
                ", user_status_date=" + user_status_date +
                '}';
    }

    public long getUser_id() {
        return user_id;
    }

    public void setUser_id(long user_id) {
        this.user_id = user_id;
    }

    public long getUser_type_id() {
        return user_type_id;
    }

    public void setUser_type_id(long user_type_id) {
        this.user_type_id = user_type_id;
    }

    public char getFirst_Name() {
        return First_Name;
    }

    public void setFirst_Name(char first_Name) {
        First_Name = first_Name;
    }

    public char getMiddle_Name() {
        return Middle_Name;
    }

    public void setMiddle_Name(char middle_Name) {
        Middle_Name = middle_Name;
    }

    public char getLast_Name() {
        return Last_Name;
    }

    public void setLast_Name(char last_Name) {
        Last_Name = last_Name;
    }

    public char getEmail_id() {
        return Email_id;
    }

    public void setEmail_id(char email_id) {
        Email_id = email_id;
    }

    public long getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(long phone_number) {
        this.phone_number = phone_number;
    }

    public char getMobile_number() {
        return Mobile_number;
    }

    public void setMobile_number(char mobile_number) {
        Mobile_number = mobile_number;
    }

    public char getAddress_Ln1() {
        return Address_Ln1;
    }

    public void setAddress_Ln1(char address_Ln1) {
        Address_Ln1 = address_Ln1;
    }

    public char getAddress_Ln2() {
        return Address_Ln2;
    }

    public void setAddress_Ln2(char address_Ln2) {
        Address_Ln2 = address_Ln2;
    }

    public char getAddress_Ln3() {
        return Address_Ln3;
    }

    public void setAddress_Ln3(char address_Ln3) {
        Address_Ln3 = address_Ln3;
    }

    public long getCity() {
        return city;
    }

    public void setCity(long city) {
        this.city = city;
    }

    public char getCountry() {
        return country;
    }

    public void setCountry(char country) {
        this.country = country;
    }

    public long getZipcode() {
        return zipcode;
    }

    public void setZipcode(long zipcode) {
        this.zipcode = zipcode;
    }

    public Date getRegistered_date() {
        return Registered_date;
    }

    public void setRegistered_date(Date registered_date) {
        Registered_date = registered_date;
    }

    public long getUser_status_id() {
        return user_status_id;
    }

    public void setUser_status_id(long user_status_id) {
        this.user_status_id = user_status_id;
    }

    public Date getUser_status_date() {
        return user_status_date;
    }

    public void setUser_status_date(Date user_status_date) {
        this.user_status_date = user_status_date;
    }

    public char getEmergency_availability_ind() {
        return emergency_availability_ind;
    }

    public void setEmergency_availability_ind(char emergency_availability_ind) {
        this.emergency_availability_ind = emergency_availability_ind;
    }
}
