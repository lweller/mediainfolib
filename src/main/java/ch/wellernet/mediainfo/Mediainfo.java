/**
 *
 */
package ch.wellernet.mediainfo;

import static java.lang.Long.parseLong;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.Duration;

/**
 * @author Lucien Weller <lucien@wellernet.ch>
 */
public class Mediainfo {
    private static final Log LOG = LogFactory.getLog(Mediainfo.class);

    /**
     * Path to mediainfo , default to <code>mediainfo</code>, assuming that path to mediainfo is in <code>PATH</code> environment variable
     */
    private final String executable;

    public Mediainfo(String executable) {
        this.executable = executable == null ? "mediainfo" : executable;
    }

    /**
     * Determines the duration of media item.
     *
     * @param file
     *            the file of media item
     * @return the duration of media item or <code>null</code> it duration cannot be determined
     */
    public Duration determineVideoDuration(File file) {
        String result = "";
        try {
            String command = format("\"%s\" --Output=\"Video;%%Duration%%\" %s", executable, file.getAbsolutePath());
            result = executeCommand(command);
            return new Duration(parseLong(result));
        } catch (NumberFormatException exception) {
            LOG.warn(format("'%s' is not a valid duration", result));
            return null;
        }
    }

    Process createProcess(String command) throws IOException {
        return getRuntime().exec(command);
    }

    String executeCommand(String command) {
        Process process;
        try {
            process = createProcess(command);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
            } else {
                LOG.warn(format("receivced an exit code different than 0 : %d", exitCode));
                return null;
            }
        } catch (InterruptedException | IOException exception) {
            LOG.warn("Caught exception", exception);
            return null;
        }
    }
}
