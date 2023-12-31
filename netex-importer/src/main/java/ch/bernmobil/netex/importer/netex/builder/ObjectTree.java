package ch.bernmobil.netex.importer.netex.builder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ch.bernmobil.netex.importer.xml.MultilingualStringParser.MultilingualString;

public class ObjectTree {

	private final Map<String, List<Object>> tree;

	private ObjectTree(Object object) {
		this.tree = mapObjectToTree(object);
	}

	public static ObjectTree of(Object object) {
		return new ObjectTree(object);
	}

	public ObjectTree child(String childName) {
		return new ObjectTree(getSingleChild(tree, childName));
	}

	public ObjectTree optionalChild(String childName) {
		final Object child = getSingleOptionalChild(tree, childName);
		if (child != null) {
			return new ObjectTree(child);
		} else {
			return null;
		}
	}

	public List<ObjectTree> children(String childrenName) {
		final List<Object> list = tree.get(childrenName);
		if (list == null) {
			return Collections.emptyList();
		} else {
			return list.stream().map(ObjectTree::new).toList();
		}
	}

	public String text(String childName) {
		return (String) getSingleChild(tree, childName);
	}

	public String optionalText(String childName) {
		return (String) getSingleOptionalChild(tree, childName);
	}

	public MultilingualString multilingualString(String childName) {
		return (MultilingualString) getSingleChild(tree, childName);
	}

	public Optional<MultilingualString> optionalMultilingualString(String childName) {
		return Optional.ofNullable((MultilingualString) getSingleOptionalChild(tree, childName));
	}

	@SuppressWarnings("unchecked")
	private static Map<String, List<Object>> mapObjectToTree(Object object) {
		return (Map<String, List<Object>>) object;
	}

	private static Object getSingleChild(Map<String, List<Object>> tree, String childName) {
		final List<Object> list = tree.get(childName);
		if (list == null) {
			throw new IllegalArgumentException("child " + childName + " not defined");
		} else if (list.isEmpty()) {
			throw new IllegalArgumentException("child " + childName + " not found");
		} else if (list.size() > 1) {
			throw new IllegalArgumentException("more than one child " + childName + " found");
		} else {
			final Object result = list.get(0);
			if (result == null) {
				throw new IllegalArgumentException("child " + childName + " is empty");
			} else {
				return result;
			}
		}
	}

	private static Object getSingleOptionalChild(Map<String, List<Object>> tree, String childName) {
		final List<Object> list = tree.get(childName);
		if (list == null || list.isEmpty()) {
			return null;
		} else if (list.size() > 1) {
			throw new IllegalArgumentException("more than one child " + childName + " found");
		} else {
			return list.get(0);
		}
	}
}
