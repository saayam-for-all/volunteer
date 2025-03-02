package org.sfa.volunteer.config;

public class DbContextHolder {
    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    public static void setDb(String db) {
        CONTEXT.set(db);
    }

    public static String getDb() {
        String dbKey = CONTEXT.get();
        return dbKey;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
