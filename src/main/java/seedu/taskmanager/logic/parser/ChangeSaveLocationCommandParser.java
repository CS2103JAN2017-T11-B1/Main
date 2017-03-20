package seedu.taskmanager.logic.parser;

import static seedu.taskmanager.commons.core.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.io.File;
import java.util.Optional;

import seedu.taskmanager.logic.commands.ChangeSaveLocationCommand;
import seedu.taskmanager.logic.commands.Command;
import seedu.taskmanager.logic.commands.IncorrectCommand;

public class ChangeSaveLocationCommandParser {

    public static final String INVALID_SAVE_LOCATION = "Invalid input for save location";

    /**
     * Parses the given {@code String} of arguments in the context of the
     * ChangeSaveLocationCommand and returns an ChangeSaveLocationCommand object for execution.
     */

    public Command parse(String args) {

        assert args != null; 

        Optional<String> string = Optional.ofNullable(args);
        File saveLocation = new File(args);

        if (!string.isPresent() || !saveLocation.isDirectory()) {
            return new IncorrectCommand(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, ChangeSaveLocationCommand.MESSAGE_USAGE));
        }

        return new ChangeSaveLocationCommand(saveLocation);
    }

}