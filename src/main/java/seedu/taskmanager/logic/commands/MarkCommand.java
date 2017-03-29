package seedu.taskmanager.logic.commands;

import java.util.List;
import java.util.NoSuchElementException;

import seedu.taskmanager.commons.core.Messages;
import seedu.taskmanager.logic.commands.exceptions.CommandException;
import seedu.taskmanager.model.task.ReadOnlyTask;
import seedu.taskmanager.model.task.UniqueTaskList;

//@@author A0139520L
/**
 * Updates the details of an existing task in the task manager.
 */
public class MarkCommand extends Command {

    public static final String COMMAND_WORD = "MARK";

    public static final String MESSAGE_USAGE = COMMAND_WORD + ": Marks task identified "
            + "by the index number used in the last task listing as complete. "
            + "Existing values will be overwritten by the input values.\n"
            + "Parameters: INDEX (must be a positive integer)\n" + "Example: " + COMMAND_WORD + " 1 ";

    public static final String MESSAGE_MARK_TASK_SUCCESS = "Marked Task: %1$s";
    public static final String MESSAGE_DUPLICATE_TASK = "This task has already been completed.";
    public static final String MESSAGE_ALREADY_MARKED = "This task is already marked as complete.";

    private final int filteredTaskListIndex;

    /**
     * @param filteredTaskListIndex
     *            the index of the task in the filtered task list to update
     * @param updateTaskDescriptor
     *            details to update the task with
     */
    public MarkCommand(int filteredTaskListIndex) {
        assert filteredTaskListIndex > 0;

        // converts filteredTaskListIndex from one-based to zero-based.
        this.filteredTaskListIndex = filteredTaskListIndex - 1;

    }

    @Override
    public CommandResult execute() throws CommandException {
        List<ReadOnlyTask> lastShownList = model.getFilteredTaskList();

        if (filteredTaskListIndex >= lastShownList.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_TASK_DISPLAYED_INDEX);
        }

        ReadOnlyTask taskToMark = lastShownList.get(filteredTaskListIndex);

        try {
            model.markTask(filteredTaskListIndex);
        } catch (UniqueTaskList.DuplicateTaskException dpe) {
            throw new CommandException(MESSAGE_DUPLICATE_TASK);
        } catch (NoSuchElementException nsee) {
            throw new CommandException(MESSAGE_ALREADY_MARKED);
        }

        model.updateFilteredListToShowAll();
        return new CommandResult(String.format(MESSAGE_MARK_TASK_SUCCESS, taskToMark));
    }

}
