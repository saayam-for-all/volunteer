package org.sfa.volunteer.exception;

/** Profile requests may echo wizard progress, but only onboarding operations advance it. */
public class WizardProgressMutationException extends IllegalArgumentException {
    public WizardProgressMutationException() {
        super("Wizard progress is managed by onboarding");
    }
}
