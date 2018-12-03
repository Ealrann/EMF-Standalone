module org.eclipse.emf.ecore.xmi
{
	exports org.eclipse.emf.ecore.xmi;
	exports org.eclipse.emf.ecore.xmi.impl;
	exports org.eclipse.emf.ecore.xmi.util;

	requires java.xml;
	
	requires transitive org.eclipse.emf.common;
	requires transitive org.eclipse.emf.ecore;
}
