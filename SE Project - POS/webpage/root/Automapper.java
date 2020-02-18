package root;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;

import edu.wit.se16.networking.requests.IRequest;
import edu.wit.se16.networking.requests.RequestPage;
import edu.wit.se16.system.logging.LoggingUtil;

public class Automapper {
	private static final Logger LOG = LoggingUtil.getLogger();

	/**
	 * 	Uses this package as the root directory for the server
	 * 	@see Automapper#loadPackageAsRoot(String)
	 */
	public static Map<String, IRequest> loadPackage() {
		return loadPackageAsRoot(Automapper.class.getPackage().getName());
	}
	
	/**
	 * 	Takes in the fully-qualified name of a package, and attempts to load all sub-files 
	 * 	as requests.
	 * 
	 * 	@return 
	 * 		A map of resource locations to {@link IRequest} instances <br />
	 * 		Locations are '/' separated path-names, following the provided root-package
	 * 
	 * 	@see Automapper#buildRequest(ClassLoader, File, String)
	 * 	@see Package#getName()
	 */
	public static Map<String, IRequest> loadPackageAsRoot(String packageName) {
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			
			// convert from package name to file-path
			String packageFile = packageName.replace('.', '/');
			Enumeration<URL> resources = loader.getResources(packageFile);
			
			if(resources == null) {
				LOG.error("Unable to find specified package");
				return null;
			}
			
			Map<String, IRequest> request_map = null;
			
			// for all of the sub-packages
			while(resources.hasMoreElements()) {
				URL res = resources.nextElement();
				request_map = mapAllResources(loader, res, packageName, "", request_map);
			}
			
			return request_map;
			
		} catch(IOException e) {
			LOG.error("Unable to load package resources", e);
			return null;
		}
	}
	
	private static Map<String, IRequest> mapAllResources(ClassLoader loader, URL dir, String rootPackage, String rootPath, Map<String, IRequest> mapping) {
		if(mapping == null) mapping = new HashMap<>();
		
		try {
			URLConnection connection = dir.openConnection();
			// if this is being run from inside a Jar
			if(connection instanceof JarURLConnection) {
				JarFile jar = ((JarURLConnection) connection).getJarFile();
				mapping = mapJarResources(loader, jar, rootPackage, rootPath, mapping);
				
			// if not, most likely run from a file-system
			} else {
				File folder = new File(URLDecoder.decode(dir.getFile(), "UTF-8"));
				
				// search for sub files/folders
				for(File file : folder.listFiles()) {
					URL fURL = file.toURI().toURL();
					
					// if sub-folder
					if(file.isDirectory()) {
						// recursive search
						mapAllResources(loader, fURL, rootPackage + '.' + file.getName(), rootPath + '.' + file.getName(), mapping);
						
					} else {
						// attempt to build a request from file
						IRequest request = buildRequest(loader, fURL, rootPackage);
						
						if(request != null) {
							// convert package-path into URL extension 
							String path = rootPath + '.' + request.getCommand();
							mapping.put(path.replace('.', '/'), request);
						}
					}
				}
			}
		} catch (IOException e) {
			// Catch error and print message
			LOG.error("Unable to map directory {}", dir);
			LOG.error("Caused by: ", e);
			// return whatever mappings we have so far
		}
		
		return mapping;
	}
	
	private static Map<String, IRequest> mapJarResources(ClassLoader loader, JarFile jar, String root, String rootPath, Map<String, IRequest> mapping) {
		Enumeration<JarEntry> entries = jar.entries();
		
		while(entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			String entryPath = entry.getName();
			
			if(!entry.isDirectory() && entryPath.startsWith(root)) {
				// get resource-URL for jar-entry
				URL fURL = loader.getResource(entryPath);
				if(fURL == null) {
					LOG.trace("Unable to map jar-entry \"{}\", cannot find resource", entryPath);
					continue;
				}
				
				// calculate the parent directory
				String rootPackage = new File(entryPath).getParent().replaceAll("[/\\\\]", ".");
				// calculate the extension past the root package
				String pathExtension = rootPackage.substring(root.length());
				
				// attempt to build a request from file
				IRequest request = buildRequest(loader, fURL, rootPackage);
				
				if(request != null) {
					// convert package-path into URL extension 
					String path = rootPath + pathExtension + '.' + request.getCommand();
					mapping.put(path.replace('.', '/'), request);
				}
			}
		}
		
		return mapping;
	}
	
	/**
	 * 	Attempts to create a IRequest instance from provided file <br /><br />
	 * 
	 * 	If file represents a {@code class} and that class is an instance of {@link IRequest}
	 * 	then an instance of the class is created.
	 * 
	 * 	If file represents an HTML file, then a {@link RequestPage} instance is created with reference to 
	 * 	provided file. <br /><br />
	 * 
	 * 	Else {@code null} is returned.
	 */
	private static IRequest buildRequest(ClassLoader loader, URL src, String path) {
		String fileName = src.getFile();
		fileName = fileName.replaceAll("\\\\", "/").substring(fileName.lastIndexOf('/') + 1);
		
		// replace everything before the first '.' with "<empty-string>"
		String fileType = fileName.replaceAll("^[^.]*\\.", "");
		// remove file extension from file-name
		fileName = fileName.substring(0, fileName.length() - fileType.length() - 1);
		
		switch(fileType) {
			case "class":
				try {
					// load class-object from file-name
					Class<?> clazz = loader.loadClass(path + '.' + fileName);
					
					// check if class is a Request <implements IRequest>
					if(!IRequest.class.isAssignableFrom(clazz)) {
						LOG.trace("Skipping class mapping for \"{}.{}\" not a Request", path, fileName);
						return null;
					}
					
					LOG.trace("Mapping Class resource \"{}.{}\"", path, fileName);
	
					// create a new Instance of the class
					@SuppressWarnings("unchecked")
					Class<? extends IRequest> requestClazz = (Class<? extends IRequest>) clazz;
					IRequest request = requestClazz.newInstance();
					return request;
					
				} catch(ClassNotFoundException e) {
					LOG.warn("Unable to map class \"{}.{}\" failed to load/find class", path, fileName);
					return null; // don't want to fall through to other switch-case
					
				} catch(InstantiationException | IllegalAccessException e) {
					LOG.error("Failed to instantiate class \"{}.{}\"", path, fileName);
					LOG.warn("Make sure there's a default (no-args) constructor on \"{}.{}\"", path, fileName);
					return null; // don't want to fall through to other switch-case
				}
				
			case "html":
				LOG.trace("Mapping HTML resource \"{}.{}\"", path, fileName);
				return new RequestPage(path.replace('.', '/') + "/" + fileName + "." + fileType, fileName);
				
			default:
				LOG.trace("Skipping resource mapping for \"{}.{}\" detected-type [{}]", path, fileName, fileType);
				return null;
		}
	}
}
