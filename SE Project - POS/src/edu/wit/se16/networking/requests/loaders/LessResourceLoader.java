package edu.wit.se16.networking.requests.loaders;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.lesscss.LessCompiler;
import org.lesscss.LessException;
import org.lesscss.LessSource;
import org.lesscss.Resource;
import org.slf4j.Logger;

import edu.wit.se16.system.logging.LoggingUtil;

public class LessResourceLoader {
	private static final Logger LOG = LoggingUtil.getLogger();
	private static final LessCompiler COMPILER = new LessCompiler();
	
	public static String loadLess(String resourcePath) throws IOException {
		try {
			Resource resource = new ClasspathResource(resourcePath);
			LessSource src = new LessSource(resource);
			
			LOG.trace("Compiling LESS \"{}\"...", resourcePath);
			return COMPILER.compile(src);
			
		} catch (LessException e) {
			LOG.error("Could not compile LESS resource!", e);
			return null;
		}
	}
	
	private static class ClasspathResource implements Resource {
		private String src_path;
		private URL resourceURL;
		private String name;
		
		public ClasspathResource(String resourcePath) {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			this.resourceURL = loader.getResource(resourcePath);
			
			// extract file name
			int index = resourcePath.lastIndexOf('/');
			if(index < 0) index = 0;
			this.name = resourcePath.substring(index);
		}
		
		public boolean exists() {
			return resourceURL != null;
		}

		public InputStream getInputStream() throws IOException {
			return resourceURL.openStream();
		}

		public Resource createRelative(String relativeResourcePath) throws IOException {
			return new ClasspathResource(RelativePathUtil.calculatePath(src_path, relativeResourcePath));
		}

		public String getName() { return name; }
		public long lastModified() { return 0; }

	}

//	----------------------------------------- Non-Constructible ----------------------------------------- \\
	private LessResourceLoader() { }
}
