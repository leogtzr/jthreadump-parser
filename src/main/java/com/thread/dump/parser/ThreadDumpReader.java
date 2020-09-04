// TODO: remove main class
// TODO: add a sample app using the lib?
package com.thread.dump.parser;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.thread.dump.parser.domain.StackTraceLock;
import com.thread.dump.parser.domain.ThreadInfo;
import com.thread.dump.parser.util.ParsingConstants;
import com.thread.dump.parser.util.ThreadParsing;
import org.apache.commons.lang3.StringUtils;

import static com.thread.dump.parser.util.ParsingConstants.NEW_LINE;

/**
 * @author Leo Gutiérrez (leogutierrezramirez@gmail.com)
 */
public final class ThreadDumpReader {

	private static List<ThreadInfo> parse(final List<String> tlines) {
		final List<ThreadInfo> threads = new ArrayList<>();

		for (int i = 0; i < tlines.size(); i++) {

			String line = tlines.get(i);
			if (line.startsWith(ParsingConstants.THREAD_INFORMATION_BEGIN)) {

				final Optional<ThreadInfo> threadInfo = ThreadParsing.extractThreadInfoFromLine(line);

				if (threadInfo.isPresent()) {
					final ThreadInfo thread = threadInfo.get();

					if (ThreadParsing.hasRunnableState(line)) {
						thread.setState("runnable");
					}

					i++;
					line = tlines.get(i);
					final Optional<Thread.State> state = ThreadParsing.extractThreadState2(line);
					if (state.isPresent()) {
						thread.setState(state.get().toString());
						line = tlines.get(i++);
					} else {
						i--;
					}

					if (line.startsWith(ParsingConstants.THREAD_INFORMATION_BEGIN)) {
						final String line2 = line;
						final Optional<ThreadInfo> threadInfo2 = ThreadParsing.extractThreadInfoFromLine(line);
						threadInfo2.ifPresent(th -> {
							if (ThreadParsing.hasRunnableState(line2)) {
								th.setState("runnable");
							} else if (ThreadParsing.hasWaitingOnConditionState(line2)) {
								th.setState("waiting on condition");
							}
							threads.add(th);
						});
						i++;
					}

					line = tlines.get(i);

					final StringBuilder sb = new StringBuilder();
					while (StringUtils.isNotBlank(line) && !line.startsWith(ParsingConstants.THREAD_INFORMATION_BEGIN)) {
						sb.append(line.trim()).append(NEW_LINE);
						i++;
						line = tlines.get(i);
					}

					if (StringUtils.isNotEmpty(sb.toString())) {
						thread.setStackTrace(sb.toString());
					}

					threads.add(thread);
				}
			}

		}

		return threads;
	}

	private static List<ThreadInfo> read(final Reader reader) throws IOException {
		final List<String> lines = new ArrayList<>();
		try (final BufferedReader br = new BufferedReader(reader)) {
			for (String line = br.readLine(); line != null; line = br.readLine()) {
				lines.add(line);
			}
		}

		return parse(lines);
	}

	public static List<ThreadInfo> fromFile(final String threadDumpFilePath) throws IOException {
		return read(new FileReader(threadDumpFilePath));
	}

	public static List<ThreadInfo> fromString(final String content) throws IOException {
		return read(new StringReader(content));
	}
	
	private static void printLockingThreadInformation(
			final Map<StackTraceLock, Map<String, ThreadInfo>> lockingInfo, final StackTraceLock stackTraceLock) {
		
		lockingInfo.get(stackTraceLock).forEach((k, v) -> {
			System.out.println(String.format("id: %s, thread: '%s'", k, v));
			if (lockingInfo.get(StackTraceLock.LOCKED).containsKey(k)) {
				// @PENDING
			}
		});
		
	}

	private ThreadDumpReader() {}
	
}
