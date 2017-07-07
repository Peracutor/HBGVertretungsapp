package com.eissler.micha.hbgvertretungsapp.util;

/**
 * Created by Micha.
 * 03.06.2017
 */
public interface ValidatableChild {
    boolean isValid();

    void setParent(InputValidator.ValidatorGroup validatorGroup);
}
