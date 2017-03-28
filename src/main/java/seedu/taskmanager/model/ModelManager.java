
package seedu.taskmanager.model;

import java.util.Set;
import java.util.Stack;
import java.util.logging.Logger;

import javafx.collections.transformation.FilteredList;
import seedu.taskmanager.commons.core.ComponentManager;
import seedu.taskmanager.commons.core.LogsCenter;
import seedu.taskmanager.commons.core.UnmodifiableObservableList;
import seedu.taskmanager.commons.events.model.TaskManagerChangedEvent;
import seedu.taskmanager.commons.util.CollectionUtil;
import seedu.taskmanager.commons.util.StringUtil;
import seedu.taskmanager.model.task.ReadOnlyTask;
import seedu.taskmanager.model.task.Task;
import seedu.taskmanager.model.task.UniqueTaskList;
import seedu.taskmanager.model.task.UniqueTaskList.TaskNotFoundException;

/**
 * Represents the in-memory model of the task manager data. All changes to any
 * model should be synchronized.
 */
public class ModelManager extends ComponentManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private TaskManager taskManager;
    private Stack<TaskManager> undoTaskManager;
    private Stack<TaskManager> redoTaskManager;
    private final FilteredList<ReadOnlyTask> filteredTasks;

    /**
     * Initializes a ModelManager with the given taskManager and userPrefs.
     */
    public ModelManager(ReadOnlyTaskManager taskManager, UserPrefs userPrefs) {
        super();
        assert !CollectionUtil.isAnyNull(taskManager, userPrefs);

        logger.fine("Initializing with task manager: " + taskManager + " and user prefs " + userPrefs);

        this.taskManager = new TaskManager(taskManager);
        filteredTasks = new FilteredList<>(this.taskManager.getTaskList());
        undoTaskManager = new Stack<TaskManager>();
        redoTaskManager = new Stack<TaskManager>();
    }

    public ModelManager() {
        this(new TaskManager(), new UserPrefs());
    }

    @Override
    public void resetData(ReadOnlyTaskManager newData) {
        saveInstance();
        taskManager.resetData(newData);
        indicateTaskManagerChanged();
    }

    @Override
    public ReadOnlyTaskManager getTaskManager() {
        return taskManager;
    }

    /** Raises an event to indicate the model has changed */
    private void indicateTaskManagerChanged() {
        raise(new TaskManagerChangedEvent(taskManager));
    }

    /** Re-save data when save location has changed */
    public void saveTaskManager() {
        indicateTaskManagerChanged();
    }

    /** Save a copy of task manager before data is changed. */
    private void saveInstance() {
        undoTaskManager.push(new TaskManager(taskManager));
        redoTaskManager.clear();
    }

    /** Undo previous action of task manager. */
    public void undoTaskManager() {
        TaskManager currentTaskManager = new TaskManager(taskManager);
        taskManager.resetData(undoTaskManager.peek());
        undoTaskManager.pop();
        redoTaskManager.push(currentTaskManager);
    }

    /** Undo previous action of task manager. */
    public void redoTaskManager() {
        TaskManager currentTaskManager = new TaskManager(taskManager);
        taskManager.resetData(redoTaskManager.peek());
        redoTaskManager.pop();
        undoTaskManager.push(currentTaskManager);
    }

    @Override
    public synchronized void deleteTask(ReadOnlyTask target) throws TaskNotFoundException {
        taskManager.removeTask(target);
        indicateTaskManagerChanged();
    }

    // @@author A0142418L
    @Override
    public synchronized void deleteTasksDate(UnmodifiableObservableList<ReadOnlyTask> targets)
            throws TaskNotFoundException {
        while (targets.size() != 0) {
            try {
                ReadOnlyTask taskToDelete = targets.get(0);
                saveInstance();
                taskManager.removeTask(taskToDelete);
            } catch (TaskNotFoundException pnfe) {
                assert false : "The target task cannot be missing";
            }
        }
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
    }

    @Override
    public synchronized void deleteTasksName(UnmodifiableObservableList<ReadOnlyTask> targets, String toDeleteTaskName)
            throws TaskNotFoundException {
        while (targets.size() != 0) {
            try {
                ReadOnlyTask taskToDelete = targets.get(0);
                if (toDeleteTaskName.equals(taskToDelete.getTaskName().fullTaskName)) {
                    saveInstance();
                    taskManager.removeTask(taskToDelete);
                    break;
                }
            } catch (TaskNotFoundException pnfe) {
                assert false : "The target task cannot be missing";
            }
        }
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
    }

    // @@author
    @Override
    public synchronized void addTask(Task task) throws UniqueTaskList.DuplicateTaskException {
        saveInstance();
        taskManager.addTask(task);
        updateFilteredListToShowAll();
        indicateTaskManagerChanged();
    }

    @Override
    public void updateTask(int filteredTaskListIndex, ReadOnlyTask editedTask)
            throws UniqueTaskList.DuplicateTaskException {
        assert editedTask != null;

        saveInstance();
        int taskManagerIndex = filteredTasks.getSourceIndex(filteredTaskListIndex);
        taskManager.updateTask(taskManagerIndex, editedTask);
        indicateTaskManagerChanged();
    }

    // @@author A0139520L
    @Override
    public void markTask(int filteredTaskListIndex) throws UniqueTaskList.DuplicateTaskException {

        saveInstance();
        int taskManagerIndex = filteredTasks.getSourceIndex(filteredTaskListIndex);
        taskManager.markTask(taskManagerIndex, true);
        indicateTaskManagerChanged();
    }

    // @@author A0139520L
    @Override
    public void unmarkTask(int filteredTaskListIndex) throws UniqueTaskList.DuplicateTaskException {

        saveInstance();
        int taskManagerIndex = filteredTasks.getSourceIndex(filteredTaskListIndex);
        taskManager.markTask(taskManagerIndex, false);
        indicateTaskManagerChanged();
    }
    // =========== Filtered Task List Accessors
    // =============================================================

    // @@author
    @Override
    public UnmodifiableObservableList<ReadOnlyTask> getFilteredTaskList() {
        return new UnmodifiableObservableList<>(filteredTasks);
    }

    @Override
    public void updateFilteredListToShowAll() {
        filteredTasks.setPredicate(null);
        updateFilteredTaskListToShowByCompletion(false);
    }

    @Override
    public void updateFilteredTaskList(Set<String> keywords) {
        updateFilteredTaskList(new PredicateExpression(new TaskQualifier(keywords)));
    }

    @Override
    public void updateFilteredTaskListForListCommand(Set<String> keywords, boolean isComplete) {
        updateFilteredTaskList(new PredicateExpression(new ListQualifier(keywords, isComplete)));
    }

    // @@author A0139520L
    @Override
    public void updateFilteredTaskListToShowByCompletion(boolean isComplete) {
        updateFilteredTaskList(new PredicateExpression(new CompletedQualifier(isComplete)));
    }

    // @@author
    private void updateFilteredTaskList(Expression expression) {
        filteredTasks.setPredicate(expression::satisfies);
    }

    // ========== Inner classes/interfaces used for filtering
    // =================================================

    interface Expression {
        boolean satisfies(ReadOnlyTask task);

        String toString();
    }

    private class PredicateExpression implements Expression {

        private final Qualifier qualifier;

        PredicateExpression(Qualifier qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public boolean satisfies(ReadOnlyTask task) {
            return qualifier.run(task);
        }

        @Override
        public String toString() {
            return qualifier.toString();
        }
    }

    interface Qualifier {
        boolean run(ReadOnlyTask task);

        String toString();
    }

    private class TaskQualifier implements Qualifier {
        private Set<String> taskKeyWords;

        TaskQualifier(Set<String> taskKeyWords) {
            this.taskKeyWords = taskKeyWords;
        }

        public boolean run(ReadOnlyTask task) {
            return (taskKeyWords.stream()
                    .filter(keyword -> StringUtil.containsWordIgnoreCase(task.getTaskName().fullTaskName, keyword))
                    .findAny().isPresent())
                    || (taskKeyWords.stream()
                            .filter(keyword -> StringUtil.containsWordIgnoreCase(task.getStartDate().value, keyword))
                            .findAny().isPresent())
                    || (taskKeyWords.stream()
                            .filter(keyword -> StringUtil.containsWordIgnoreCase(task.getStartTime().value, keyword))
                            .findAny().isPresent())
                    || (taskKeyWords.stream()
                            .filter(keyword -> StringUtil.containsWordIgnoreCase(task.getEndDate().value, keyword))
                            .findAny().isPresent())
                    || (taskKeyWords.stream()
                            .filter(keyword -> StringUtil.containsWordIgnoreCase(task.getEndTime().value, keyword))
                            .findAny().isPresent());
        }

        @Override
        public String toString() {
            return "task name=" + String.join(", ", taskKeyWords);
        }
    }

    // @@author A0139520L
    private class CompletedQualifier implements Qualifier {
        private boolean isComplete;

        CompletedQualifier(boolean isComplete) {
            this.isComplete = isComplete;
        }

        public boolean run(ReadOnlyTask task) {
            return (task.getIsMarkedAsComplete().equals(isComplete));
        }

        @Override
        public String toString() {
            return "task name=" + String.join(", ", "w");
        }
    }

    private class ListQualifier implements Qualifier {
        private boolean isComplete;
        private Set<String> taskKeyWords;

        ListQualifier(Set<String> taskKeyWords, boolean isComplete) {
            this.taskKeyWords = taskKeyWords;
            this.isComplete = isComplete;
        }

        public boolean run(ReadOnlyTask task) {
            return (task.getIsMarkedAsComplete().equals(isComplete)) && (taskKeyWords.stream()
                    .filter(keyword -> StringUtil.containsWordIgnoreCase(task.getStartDate().value, keyword)).findAny()
                    .isPresent());
            // && (taskKeyWords.stream()
            // .filter(keyword ->
            // StringUtil.containsWordIgnoreCase(task.getEndDate().value,
            // keyword))
            // .findAny().isPresent());
        }

        @Override
        public String toString() {
            return "task name=" + String.join(", ", taskKeyWords);
        }
    }
}
