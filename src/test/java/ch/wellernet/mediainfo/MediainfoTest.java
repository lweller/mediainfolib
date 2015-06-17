package ch.wellernet.mediainfo;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.System.arraycopy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class MediainfoTest {

    private static final String MEDIAINFO_EXECUTABLE = "mediainfo";

    private static final File MEDIA_FILE = new File("/path/to/film.avi");

    private static final String EXPECTED_VIDEODURATION_COMMAND = format("\"%s\" --Output=\"Video;%%Duration%%\" %s", MEDIAINFO_EXECUTABLE,
            MEDIA_FILE.getAbsolutePath());

    // undes test
    @Spy
    @InjectMocks
    private Mediainfo mediainfo;

    @Mock
    private Process process;

    @Mock
    private InputStream inputStream;

    @Before
    public void setup() throws IOException {
        initMocks(this);

        doReturn(inputStream).when(process).getInputStream();
        doReturn(process).when(mediainfo).createProcess(anyString());
    }

    @Test
    public void shouldDetermineVideoDuration() {
        // given
        doReturn("42000").when(mediainfo).executeCommand(anyString());

        // when
        Duration duration = mediainfo.determineVideoDuration(MEDIA_FILE);

        // then
        assertThat(duration, is(new Duration(42000l)));
        verify(mediainfo).executeCommand(EXPECTED_VIDEODURATION_COMMAND);
    }

    @Test
    public void shouldNullValueIfIOExceptonOccursWhileExecutingCommand() throws IOException {
        // given
        doThrow(IOException.class).when(inputStream).read(any(byte[].class), anyInt(), anyInt());

        // when
        String result = mediainfo.executeCommand("command");

        // then
        assertThat(result, is(nullValue()));
    }

    @Test
    public void shouldNullValueIfProcessExistsWithValueDifferentToZero() throws InterruptedException {
        // given
        doReturn(-1).when(process).waitFor();

        // when
        String result = mediainfo.executeCommand("command");

        // then
        assertThat(result, is(nullValue()));
    }

    @Test
    public void shouldReturnNullAsVideoDurationIfProcessResultIsNullValue() {
        // given
        doReturn(null).when(mediainfo).executeCommand(anyString());

        // when
        Duration duration = mediainfo.determineVideoDuration(MEDIA_FILE);

        // then
        assertThat(duration, is(nullValue()));
    }

    @Test
    public void shouldReturnNullAsVideoDurationIfProcessResultValueIsNotParsable() {
        // given
        doReturn("something").when(mediainfo).executeCommand(anyString());

        // when
        Duration duration = mediainfo.determineVideoDuration(MEDIA_FILE);

        // then
        assertThat(duration, is(nullValue()));
    }

    @Test
    public void shouldReturnResultOfExecutedCommand() throws IOException {
        // given
        final String expectedResult = "result";
        doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) throws Throwable {
                byte[] stream = (expectedResult + "\n").getBytes();
                byte[] buffer = invocation.getArgumentAt(0, byte[].class);
                arraycopy(stream, 0, buffer, 0, min(stream.length, buffer.length));
                return min(stream.length, buffer.length);
            }
        }).when(inputStream).read(any(byte[].class), anyInt(), anyInt());

        // when
        String result = mediainfo.executeCommand("command");

        // then
        assertThat(result, is(expectedResult));
    }
}
