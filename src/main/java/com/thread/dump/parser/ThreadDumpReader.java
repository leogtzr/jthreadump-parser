// TODO: remove main class
// TODO: add a sample app using the lib?
package com.thread.dump.parser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
				Optional<ThreadInfo> threadInfoTogether = Optional.empty();
				final Optional<ThreadInfo> threadInfo = ThreadParsing.extractThreadInfoFromLine(line);

				if (threadInfo.isPresent()) {
					boolean threadTogether = false;
					final ThreadInfo thread = threadInfo.get();

					if (ThreadParsing.hasRunnableState(line)) {
						thread.setState("runnable");
					}

					i++;
					if (i < tlines.size()) {
						line = tlines.get(i);
					} else {
						break;
					}

					// Look for the thread state:
					final Optional<Thread.State> state = ThreadParsing.extractThreadState(line);
					if (state.isPresent()) {
						thread.setState(state.get().toString());
						line = tlines.get(i++);
					} else {
						i--;
					}

					// There could be two threads together without a thread state ...
					if (line.startsWith(ParsingConstants.THREAD_INFORMATION_BEGIN)) {
						final String line2 = line;
						threadInfoTogether = ThreadParsing.extractThreadInfoFromLine(line);
						threadTogether = true;
						threadInfoTogether.ifPresent(th -> {
							if (ThreadParsing.hasRunnableState(line2)) {
								th.setState("runnable");
							} else if (ThreadParsing.hasWaitingOnConditionState(line2)) {
								th.setState("waiting on condition");
							}
						});
						i++;
					}

					if (i < tlines.size()) {
						line = tlines.get(i);
					} else {
						break;
					}

					final StringBuilder sb = new StringBuilder();
					while (StringUtils.isNotBlank(line) && !line.startsWith(ParsingConstants.THREAD_INFORMATION_BEGIN)) {
						sb.append(line.trim()).append(NEW_LINE);
						i++;
						if (i < tlines.size()) {
							line = tlines.get(i);
						} else {
							break;
						}
					}

					if (StringUtils.isNotEmpty(sb.toString())) {
						thread.setStackTrace(sb.toString());
					}

					if (threadTogether) {
						threads.add(thread);
						threadInfoTogether.ifPresent(th -> threads.add(th));
					} else {
						threads.add(thread);
					}
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
