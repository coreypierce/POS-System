package edu.wit.se16.system.logging.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;

import ch.qos.logback.classic.Level;

public class StreamTypes {
	
	// ===================================== Log Writer ===================================== \\
	
	public static class LogWriter extends Writer {
		private Logger LOG;
		private Level level;
		
		private StringBuffer buffer;
		
		public LogWriter(Logger LOG, Level level) {
			this.level = level;
			this.LOG = LOG;
			
			this.buffer = new StringBuffer();
		}
		
		public void write(char[] cbuf, int off, int len) throws IOException {
			this.buffer.append(cbuf, off, len);
		}

		public void flush() throws IOException {
			switch (level.levelInt) {
				case Level.INFO_INT: LOG.info(buffer.toString()); break;
				case Level.WARN_INT: LOG.warn(buffer.toString()); break;
				case Level.ERROR_INT: LOG.error(buffer.toString()); break;
				case Level.DEBUG_INT: LOG.debug(buffer.toString()); break;
				case Level.TRACE_INT: LOG.trace(buffer.toString()); break;
				
				default: throw new IllegalArgumentException("Unsupported log-level: " + (level != null ? level.levelStr : "null"));
			}
			
			buffer.setLength(0);
		}

		public void close() throws IOException {
			flush();
		}
	}
	
	// ===================================== Pipe Stream ===================================== \\

	/**
	 * 	Create a connected pair of Input/Output Streams. 
	 * 	Anything written to the OutputStream will be available to read from the InputStream.
	 */
	public static class PipeStreamFactory {
		public static final int EOS = -1;
		
		private InputStream in;
		private OutputStream out;
		
		private int start, end;
		private ByteBuffer buffer;
		private int buffer_size;
		
		private boolean streamOpen;
		
		public PipeStreamFactory(int pipe_buffer_size) throws IOException {
			this.buffer = ByteBuffer.allocate(pipe_buffer_size);
			this.buffer_size = pipe_buffer_size;
			start = end = 0;

			this.streamOpen = true;
			
			this.in = new PipeInputStream();
			this.out = new PipeOutputStream();
		}
		
		public InputStream getInputStream() { return in; }
		public OutputStream getOutputStream() { return out; }
		
		private int getReadableCount() { return start <= end ? end - start : buffer_size - (start - end); }
		private int getWriteableCount() { return buffer_size - getReadableCount() - 1; }
		
		// =============== Input Stream =============== \\
		
		private class PipeInputStream extends InputStream {
			private synchronized void waitForData() throws InterruptedIOException {
				try {
					// wait for notification
					wait();
				} catch(InterruptedException e) {
					// if we're interrupted, nothing we can do
					throw new InterruptedIOException();
				}
			}
			
			/**
			 * 	Notifies the OutputStream that more data is available
			 */
			private void notifySpaceAvailable() {
				synchronized(out) {
					// notify-one thread that space is available
					out.notify();	
				}
			}
			
			private synchronized int read_unsafe() {
				int data = buffer.get(start ++);
				// check if read-marker is past end of buffer
				if(start >= buffer_size) {
					// if so, wrap back to byte-0
					start = 0;
				}
				
				return data;
			}
			
			public int read() throws IOException {
				try {
					synchronized (this) {
						while(streamOpen && start == end) {
							waitForData();
						}
						
						// if stream is closed, return End-of-Stream
						if(!streamOpen) 
							return EOS;
						
						int data = read_unsafe();
						return data;
					}
				} finally {
					notifySpaceAvailable();
				}
			}
			
			public int read(byte[] data, int offset, int length) throws IOException {
				try {
					synchronized (this) {
						// check offset
						if(offset < 0)
							throw new IllegalArgumentException("Array-Offset cannot be < 0");
						// check buffer length
						if(offset + length > data.length)
							throw new IndexOutOfBoundsException();
						
						// wait for data to be available
						while(streamOpen && start == end) {
							waitForData();
						}
						
						// if stream is closed, return End-of-Stream
						if(!streamOpen) 
							return EOS;

						for(int i = offset, cap = offset + length; i < cap; i ++) {
							if(start != end) {
								byte b = (byte) read_unsafe();
								
								if(b != EOS) {
									data[i] = (byte) b;
									continue;
								}
								
								// else fall-through
							}

							// ran out of bytes to read
							return i - offset;
						}
						
						return length;
					}
				} finally {
					notifySpaceAvailable();
				}
			}
			
			public int available() throws IOException {
				return streamOpen ? -1 : getReadableCount();
			}
			
			public synchronized void close() throws IOException {
				streamOpen = false;
				
				notifyAll();
				notifySpaceAvailable();
			}
		}
		
		// =============== Output Stream =============== \\
		
		private class PipeOutputStream extends OutputStream {
			private synchronized void waitForSpace() throws InterruptedIOException {
				try {
					// wait for notification
					wait();
				} catch(InterruptedException e) {
					// if we're interrupted, nothing we can do
					throw new InterruptedIOException();
				}
			}
			
			/**
			 * 	Notifies the InputStream that more data is available
			 */
			private void notifyDataAvailable() {
				synchronized(in) {
					// notify-one thread that data is available
					in.notify();	
				}
			}
			
			/**
			 * 	Writes a single byte into the output-buffer, without notifying the InputStream
			 */
			private void write_quietly(int b) {
				buffer.put(end ++, (byte) b);
				// check if write-marker is past end of buffer
				if(end >= buffer_size) {
					// if so, wrap back to byte-0
					end = 0;
				}
			}

			public synchronized void write(int b) throws IOException {
				try {
					synchronized(this) {
						while(streamOpen && getWriteableCount() < 1) {
							waitForSpace();
						}
		
						// if stream is closed, then we cannot write the requested byte
						if(!streamOpen) {
							throw new IOException("Stream closed!");
						}
						
						write_quietly(b);
					}
				} finally {
					notifyDataAvailable();
				}
			}
			
			public void write(byte[] data, int offset, int length) throws IOException {
				try {
					synchronized(this) {
						if(!streamOpen) {
							throw new IOException("Stream closed!");
						}
						
						// if there isn't enough space left in the buffer
						if(getWriteableCount() < length) {
							int space = getWriteableCount();
							
							if(space > 0) {
								// write whatever we can
								write(data, offset, space);
							}
							
							// wait for there to be more space in the buffer
							while(streamOpen && getWriteableCount() < 1) {
								waitForSpace();
							}
							
							// recurse to write the rest of the data
							write(data, offset + space, length - space);
							return;
						}
						
						for(int i = offset, cap = offset + length; i < cap; i ++) {
							// wait till all bytes are written before notifying
							write_quietly(data[i]);
						}
					}
				} finally {
					notifyDataAvailable();
				}
			}
			
//			public synchronized void flush() throws IOException {
//				notifyDataAvailable();
//			}
			
			public synchronized void close() throws IOException {
				streamOpen = false;
				
				notifyAll();
				notifyDataAvailable();
			}
		}
	}
	
	// ===================================== Split Stream ===================================== \\

	/**
	 *  Allows for writing to two different {@link OutputStream OutputStreams} from one source <br />
	 *  
	 *  Used for maintaining standard-console output
	 */
	public static class SplitOutputStream extends OutputStream {
		private OutputStream a, b;
		
		public SplitOutputStream(OutputStream a, OutputStream b) {
			this.a = a;
			this.b = b;
		}
		
		public void write(byte data[], int off, int len) throws IOException {
			a.write(data, off, len);
			b.write(data, off, len);
		}

		public void write(int data) throws IOException {
			a.write(data);
			b.write(data);
		}
		
		public void flush() throws IOException {
			a.flush();
			b.flush();
		}
	}
	
	// ===================================== Message-Like Stream ===================================== \\
	
	/**
	 * 	Adapts a standard {@link OutputStream} into a text-stream that follows the standard message format
	 * 	as defined by {@link MessageLayout}. This allows for then standard System output streams to print to the logs.
	 * 
	 * 	@see System.out
	 * 	@see System.err
	 */
	public static class MessageFormatedOutputStream extends OutputStream {
		private OutputStream passthrough;
		private StringBuilder buffer;
		
		private UnaryOperator<CharSequence> formatter;
		private String messageSufix;
		
		public MessageFormatedOutputStream(OutputStream passthrough, UnaryOperator<CharSequence> formatter, String messageSufix) {
			this.passthrough = passthrough;
			this.buffer = new StringBuilder();
			
			this.formatter = formatter;
			this.messageSufix = messageSufix;
		}
		
		public void write(byte b[], int off, int len) throws IOException {
			buffer.append(new String(b, off, len));
		}

		public void write(int b) throws IOException {
			buffer.append((char) b);
		}
		
		public void flush() throws IOException {
			String data = formatter.apply(buffer) + messageSufix;
			passthrough.write(data.getBytes(StandardCharsets.UTF_8));
			
			buffer.setLength(0);
		}
	}
	
//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private StreamTypes() { }
}
