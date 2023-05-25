package com.cork.io.dao;

import com.cork.io.struct.NoteType;

import io.objectbox.annotation.Entity;

@Entity
public class ContactNoteData extends BaseNoteData {
    public String firstName;
    public String lastName;
    public String phoneNumber;
    public String emailAddress;

    public ContactNoteData() {}
}