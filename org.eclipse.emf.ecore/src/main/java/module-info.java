module org.eclipse.emf.ecore 
{
	exports org.eclipse.emf.ecore;
	exports org.eclipse.emf.ecore.impl;
	exports org.eclipse.emf.ecore.xml.namespace.impl;
	exports org.eclipse.emf.ecore.resource.impl;
	exports org.eclipse.emf.ecore.xml.type.internal;
	exports org.eclipse.emf.ecore.plugin;
	exports org.eclipse.emf.ecore.xml.namespace.util;
	exports org.eclipse.emf.ecore.xml.type;
	exports org.eclipse.emf.ecore.xml.type.util;
	exports org.eclipse.emf.ecore.xml.type.impl;
	exports org.eclipse.emf.ecore.resource;
	exports org.eclipse.emf.ecore.util;
	exports org.eclipse.emf.ecore.xml.namespace;

	requires java.xml;
	
	requires transitive org.eclipse.emf.common;
}