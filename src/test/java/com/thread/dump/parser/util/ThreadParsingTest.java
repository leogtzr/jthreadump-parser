package com.thread.dump.parser.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.thread.dump.parser.ThreadDumpReader;
import com.thread.dump.parser.domain.Locked;
import org.junit.Test;

import com.thread.dump.parser.domain.ThreadInfo;

import static org.junit.Assert.*;

/**
 * @author Leo Gutiérrez (leogutierrezramirez@gmail.com)
 */
public class ThreadParsingTest {
	
	private static final String THREAD_NAME_HEADER = 
		"\"http-nio-8080-exec-1\" #27 daemon prio=5 os_prio=0 tid=0x00007f0b8c00d000 nid=0x1d32 waiting on condition [0x00007f0bc63c3000]";
	
	private static final String THREAD_STATE = " java.lang.Thread.State: WAITING (kahsdasd)";

	private static final String DAEMON_THREAD_INFORMATION = "\"Attach Listener\" #6085 daemon prio=9 os_prio=0 tid=0x00007f90d0106000 nid=0x18a1 runnable [0x0000000000000000]" +
	"java.lang.Thread.State: RUNNABLE" +
"\n" +
"	Locked ownable synchronizers:"+
"	 - None";

	private static final String THREAD_INFO_WITH_LOCKS = "\"default task-23\" #349 prio=5 os_prio=0 tid=0x00007f8fe400c800 nid=0x72fa waiting for monitor entry [0x00007f8f7228e000]\n" +
			"\tjava.lang.Thread.State: BLOCKED (on object monitor)\n" +
			"\t at java.security.Provider.getService(Provider.java:1039)\n" +
			"\t - locked <0x0000000682e5f948> (a sun.security.provider.Sun)\n" +
			"\t at sun.security.jca.ProviderList.getService(ProviderList.java:332)\n" +
			"\t at sun.security.jca.GetInstance.getInstance(GetInstance.java:157)\n" +
			"\t at java.security.Security.getImpl(Security.java:695)\n" +
			"\t at java.security.MessageDigest.getInstance(MessageDigest.java:167)\n" +
			"\t at sun.security.rsa.RSASignature.<init>(RSASignature.java:79)\n" +
			"\t at sun.security.rsa.RSASignature$SHA512withRSA.<init>(RSASignature.java:305)\n" +
			"\t at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)\n" +
			"\t at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)\n" +
			"\t at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)\n" +
			"\t at java.lang.reflect.Constructor.newInstance(Constructor.java:423)\n" +
			"\t at java.security.Provider$Service.newInstance(Provider.java:1595)\n" +
			"\t at java.security.Signature$Delegate.newInstance(Signature.java:1020)\n" +
			"\t at java.security.Signature$Delegate.chooseProvider(Signature.java:1114)\n" +
			"\t - locked <0x00000007bc531138> (a java.lang.Object)\n" +
			"\t at java.security.Signature$Delegate.engineInitSign(Signature.java:1188)\n" +
			"\t at java.security.Signature.initSign(Signature.java:553)\n" +
			"\t at sun.security.ssl.HandshakeMessage$ECDH_ServerKeyExchange.<init>(HandshakeMessage.java:1031)\n" +
			"\t at sun.security.ssl.ServerHandshaker.clientHello(ServerHandshaker.java:971)\n" +
			"\t at sun.security.ssl.ServerHandshaker.processMessage(ServerHandshaker.java:228)\n" +
			"\t at sun.security.ssl.Handshaker.processLoop(Handshaker.java:1052)\n" +
			"\t at sun.security.ssl.Handshaker$1.run(Handshaker.java:992)\n" +
			"\t at sun.security.ssl.Handshaker$1.run(Handshaker.java:989)\n" +
			"\t at java.security.AccessController.doPrivileged(Native Method)\n" +
			"\t at sun.security.ssl.Handshaker$DelegatedTask.run(Handshaker.java:1467)\n" +
			"\t - locked <0x00000007bbbac500> (a sun.security.ssl.SSLEngineImpl)\n" +
			"\t at io.undertow.protocols.ssl.SslConduit$5.run(SslConduit.java:1021)\n" +
			"\t at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)\n" +
			"\t at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)\n" +
			"\t at java.lang.Thread.run(Thread.java:748)\n" +
			" \n" +
			"\tLocked ownable synchronizers:\n" +
			"\t - <0x00000006a43d5c08> (a java.util.concurrent.ThreadPoolExecutor$Worker)";


	@Test
	public void shouldReturnNonEmptyThreadInfoObject() {
		final Optional<ThreadInfo> threadInfo = ThreadParsing.extractThreadInfoFromLine(THREAD_NAME_HEADER);
		assertTrue(threadInfo.isPresent());
	}
	
	@Test
	public void shouldReturnNonEmptyThreadState() {
		final Optional<Thread.State> threadState = ThreadParsing.extractThreadState(THREAD_STATE);
		assertTrue(threadState.isPresent());
	}

	@Test
	public void shouldReturnThreadState() {
		final Thread.State expectedState = Thread.State.WAITING;
		final Optional<Thread.State> state = ThreadParsing.extractThreadState(THREAD_STATE);
		assertTrue(state.isPresent());
		final Thread.State threadState = state.get();
		assertEquals(expectedState, threadState);
	}

	@Test
	public void testShouldIdentifyDaemonThread() throws IOException {
		final int expectedNumberOfThreads = 1;
		final List<ThreadInfo> threads = ThreadDumpReader.fromString(DAEMON_THREAD_INFORMATION);
		assertEquals(threads.size(), expectedNumberOfThreads);
		assertTrue("Thread should be daemon", threads.get(0).isDaemon());
	}

	@Test
	public void shouldTagCorrectlyDaemonThread() {
		String expectedThreadStringInfo = "Thread Id: '0x00007f90d0106000' (daemon), Name: 'Attach Listener', State: 'RUNNABLE'";

		final ThreadInfo th = new ThreadInfo();
		th.setId("0x00007f90d0106000");
		th.setName("Attach Listener");
		th.setState("RUNNABLE");
		th.setDaemon(true);

		assertEquals(expectedThreadStringInfo, th.toString());
		th.setDaemon(false);
		expectedThreadStringInfo = "Thread Id: '0x00007f90d0106000', Name: 'Attach Listener', State: 'RUNNABLE'";
		assertEquals(expectedThreadStringInfo, th.toString());
	}

	@Test
	public void shouldRetrieveHolds() throws IOException {
		final int expectedNumberOfThreads = 1;
		final int expectedNumberOfLocksInThread = 3;

		final Locked[] expectedLocks = {
			new Locked("0x0000000682e5f948", "sun.security.provider.Sun"),
			new Locked("0x00000007bc531138", "java.lang.Object"),
			new Locked("0x00000007bbbac500", "sun.security.ssl.SSLEngineImpl")
		};

		final List<ThreadInfo> threads = ThreadDumpReader.fromString(THREAD_INFO_WITH_LOCKS);
		assertFalse(threads.isEmpty());

		final Map<ThreadInfo, List<Locked>> holds = ThreadParsing.holds(threads);
		assertEquals(expectedNumberOfThreads, holds.size());

		holds.forEach((thread, locks) -> {
			assertEquals(expectedNumberOfLocksInThread, locks.size());
			for (int i = 0; i < locks.size(); i++) {
				assertEquals(expectedLocks[i], locks.get(i));
			}
		});
	}

	@Test
	public void shouldRetrieveHoldsForThread() throws IOException {
		final int expectedNumberOfLocksInThreadWithLockInfo = 3;
		final Locked[] expectedLocks = {
				new Locked("0x0000000682e5f948", "sun.security.provider.Sun"),
				new Locked("0x00000007bc531138", "java.lang.Object"),
				new Locked("0x00000007bbbac500", "sun.security.ssl.SSLEngineImpl")
		};

		final List<ThreadInfo> threads = ThreadDumpReader.fromString(THREAD_INFO_WITH_LOCKS);
		assertFalse(threads.isEmpty());

		for (final ThreadInfo thread : threads) {
			final List<Locked> holds = ThreadParsing.holdsForThread(thread);
			assertEquals(expectedNumberOfLocksInThreadWithLockInfo, holds.size());
			for (int i = 0; i < holds.size(); i++) {
				assertEquals(expectedLocks[i], holds.get(i));
			}
		}
	}

	@Test
	public void shouldIdentifyTopMethodsInThreadDump() throws IOException {
		final List<ThreadInfo> threads = ThreadDumpReader.fromFile("samples/tdump.sample");
		assertFalse(threads.isEmpty());

		final Map<String, Integer> mostUsedMethods = ThreadParsing.mostUsedMethods(threads);

		final String javaMethodName = "java.lang.Object.wait(Native Method)";
		final int expectedNumberOfThreadsWithMethodName = 82;

		assertEquals((Integer)expectedNumberOfThreadsWithMethodName, mostUsedMethods.get(javaMethodName));
	}

	@Test
	public void shouldParseThreadDump() throws Exception {
		final List<ThreadInfo> threads = ThreadDumpReader.fromFile("samples/tdump.sample");

		final int expectedNumberOfThreads = 203;
		assertEquals(expectedNumberOfThreads, threads.size());
	}

	@Test
	public void shouldIdentifyRunnableThread() {

		final Object[][] tests = {
				// thread header info, expected result
				{"\"G1 Refine#0\" os_prio=0 cpu=3.43ms elapsed=741.86s tid=0x00007f6f5c20c000 nid=0x6b8c runnable  ", true},
				{"\"Finalizer\" #3 daemon prio=8 os_prio=0 cpu=0.77ms elapsed=741.83s tid=0x00007f6f5c29e000 nid=0x6b90 in Object.wait()  [0x00007f6f13ffe000]", false},
		};

		for (final Object[] test : tests) {
			final boolean got = ThreadParsing.hasRunnableState(test[0].toString());
			assertEquals(got, test[1]);
		}

	}

	@Test
	public void shouldIdentifyWaitingOnConditionThread() {

		final Object[][] tests = {
			// thread header info, expected result
			{"\"VM Periodic Task Thread\" os_prio=0 tid=0x00007f74bc22d800 nid=0xd9c4 waiting on condition ", true},
			{"\"Finalizer\" #3 daemon prio=8 os_prio=0 cpu=0.77ms elapsed=741.83s tid=0x00007f6f5c29e000 nid=0x6b90 in Object.wait()  [0x00007f6f13ffe000]", false},
		};

		for (final Object[] test : tests) {
			final boolean got = ThreadParsing.hasWaitingOnConditionState(test[0].toString());
			assertEquals(got, test[1]);
		}

	}

	@Test
	public void verifyNumberOfThreadsInSamples() throws Exception {
		final Object[][] tests = {
			{"samples/10.0.2.0.txt", 51}
			,{"samples/10.0.2.1.txt", 51}
			,{"samples/10.0.2.2.txt", 51}
			,{"samples/10.0.2.3.txt", 51}
			,{"samples/11.0.2.0.txt", 43}
			,{"samples/11.0.2.1.txt", 46}
			,{"samples/11.0.2.2.txt", 50}
			,{"samples/11.0.2.3.txt", 50}
			,{"samples/11.0.8.0-amazon.txt", 99}
			,{"samples/11.0.8.1-amazon.txt", 99}
			,{"samples/11.0.8.2-amazon.txt", 48}
			,{"samples/11.0.8.3-amazon.txt", 48}
			,{"samples/12.0.2.0.txt", 49}
			,{"samples/12.0.2.1.txt", 48}
			,{"samples/12.0.2.2.txt", 48}
			,{"samples/12.0.2.3.txt", 48}
			,{"samples/13.0.2.0.txt", 42}
			,{"samples/13.0.2.1.txt", 42}
			,{"samples/13.0.2.2.txt", 50}
			,{"samples/13.0.2.3.txt", 48}
			,{"samples/14.0.1.0.txt", 51}
			,{"samples/14.0.1.1.txt", 51}
			,{"samples/14.0.1.2.txt", 51}
			,{"samples/14.0.1.3.txt", 50}
			,{"samples/15.0.txt", 43}
			,{"samples/15.1.txt", 51}
			,{"samples/15.2.txt", 51}
			,{"samples/15.3.txt", 49}
			,{"samples/1.8-amazon.0.txt", 37}
			,{"samples/1.8-amazon.1.txt", 37}
			,{"samples/1.8-amazon.2.txt", 37}
			,{"samples/1.8-amazon.3.txt", 37}
			,{"samples/9.0.4.0.txt", 51}
			,{"samples/9.0.4.1.txt", 51}
			,{"samples/9.0.4.2.txt", 51}
			,{"samples/9.0.4.3.txt", 51}
			,{"samples/tdump2.sample", 58}
			,{"samples/tdump.jdk11.idea.txt", 56}
			,{"samples/tdump.sample", 203}
		};

		for (final Object[] test : tests) {
			final String sampleFile = test[0].toString();
			final int expectedCount = (Integer)test[1];

			final List<ThreadInfo> gotThreads = ThreadDumpReader.fromFile(sampleFile);
			final int gotThreadCount = gotThreads.size();

			assertEquals(expectedCount, gotThreadCount);
		}

	}
	
}
