/*
 * AS IS
 */
package ssg.serialize.tools;

import java.io.IOException;

/**
 * Represents type casting scope, priority, and casting methods.
 */
public interface ClassCast<F, T> {

    int priority();

    Class from();

    Class to();

    T cast(F obj) throws IOException;

    T tryCast(F obj, IOException[] error);
    
}
