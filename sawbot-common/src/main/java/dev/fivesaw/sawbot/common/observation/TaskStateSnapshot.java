package dev.fivesaw.sawbot.common.observation;
public final class TaskStateSnapshot {
    public static final TaskStateSnapshot UNIVERSAL = new TaskStateSnapshot("universal/0.1", false);
    private final String adapterIdentifier; private final boolean taskActive;
    public TaskStateSnapshot(String adapterIdentifier, boolean taskActive){if(adapterIdentifier==null||adapterIdentifier.isEmpty()||adapterIdentifier.length()>48)throw new IllegalArgumentException("adapterIdentifier");this.adapterIdentifier=adapterIdentifier;this.taskActive=taskActive;}
    public String adapterIdentifier(){return adapterIdentifier;} public boolean taskActive(){return taskActive;}
}
