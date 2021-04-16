package refactoringml.util;

import static org.refactoringminer.api.RefactoringType.CHANGE_ATTRIBUTE_TYPE;
import static org.refactoringminer.api.RefactoringType.CHANGE_PARAMETER_TYPE;
import static org.refactoringminer.api.RefactoringType.CHANGE_RETURN_TYPE;
import static org.refactoringminer.api.RefactoringType.CHANGE_VARIABLE_TYPE;
import static org.refactoringminer.api.RefactoringType.CONVERT_ANONYMOUS_CLASS_TO_TYPE;
import static org.refactoringminer.api.RefactoringType.EXTRACT_AND_MOVE_OPERATION;
import static org.refactoringminer.api.RefactoringType.EXTRACT_ATTRIBUTE;
import static org.refactoringminer.api.RefactoringType.EXTRACT_CLASS;
import static org.refactoringminer.api.RefactoringType.EXTRACT_INTERFACE;
import static org.refactoringminer.api.RefactoringType.EXTRACT_OPERATION;
import static org.refactoringminer.api.RefactoringType.EXTRACT_SUBCLASS;
import static org.refactoringminer.api.RefactoringType.EXTRACT_SUPERCLASS;
import static org.refactoringminer.api.RefactoringType.EXTRACT_VARIABLE;
import static org.refactoringminer.api.RefactoringType.INLINE_OPERATION;
import static org.refactoringminer.api.RefactoringType.INLINE_VARIABLE;
import static org.refactoringminer.api.RefactoringType.INTRODUCE_POLYMORPHISM;
import static org.refactoringminer.api.RefactoringType.MERGE_PARAMETER;
import static org.refactoringminer.api.RefactoringType.MERGE_VARIABLE;
import static org.refactoringminer.api.RefactoringType.MOVE_AND_INLINE_OPERATION;
import static org.refactoringminer.api.RefactoringType.MOVE_AND_RENAME_OPERATION;
import static org.refactoringminer.api.RefactoringType.MOVE_ATTRIBUTE;
import static org.refactoringminer.api.RefactoringType.MOVE_CLASS;
import static org.refactoringminer.api.RefactoringType.MOVE_OPERATION;
import static org.refactoringminer.api.RefactoringType.MOVE_RENAME_ATTRIBUTE;
import static org.refactoringminer.api.RefactoringType.MOVE_RENAME_CLASS;
import static org.refactoringminer.api.RefactoringType.MOVE_SOURCE_FOLDER;
import static org.refactoringminer.api.RefactoringType.PARAMETERIZE_VARIABLE;
import static org.refactoringminer.api.RefactoringType.PULL_UP_ATTRIBUTE;
import static org.refactoringminer.api.RefactoringType.PULL_UP_OPERATION;
import static org.refactoringminer.api.RefactoringType.PUSH_DOWN_ATTRIBUTE;
import static org.refactoringminer.api.RefactoringType.PUSH_DOWN_OPERATION;
import static org.refactoringminer.api.RefactoringType.RENAME_ATTRIBUTE;
import static org.refactoringminer.api.RefactoringType.RENAME_CLASS;
import static org.refactoringminer.api.RefactoringType.RENAME_METHOD;
import static org.refactoringminer.api.RefactoringType.RENAME_PACKAGE;
import static org.refactoringminer.api.RefactoringType.RENAME_PARAMETER;
import static org.refactoringminer.api.RefactoringType.RENAME_VARIABLE;
import static org.refactoringminer.api.RefactoringType.REPLACE_ATTRIBUTE;
import static org.refactoringminer.api.RefactoringType.REPLACE_VARIABLE_WITH_ATTRIBUTE;
import static org.refactoringminer.api.RefactoringType.SPLIT_PARAMETER;
import static org.refactoringminer.api.RefactoringType.SPLIT_VARIABLE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.eclipse.jgit.diff.Edit;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.decomposition.VariableDeclaration;
import gr.uom.java.xmi.diff.ChangeAttributeTypeRefactoring;
import gr.uom.java.xmi.diff.ChangeReturnTypeRefactoring;
import gr.uom.java.xmi.diff.ChangeVariableTypeRefactoring;
import gr.uom.java.xmi.diff.ExtractAttributeRefactoring;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.ExtractSuperclassRefactoring;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineOperationRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.MergeAttributeRefactoring;
import gr.uom.java.xmi.diff.MergeVariableRefactoring;
import gr.uom.java.xmi.diff.MoveAttributeRefactoring;
import gr.uom.java.xmi.diff.MoveOperationRefactoring;
import gr.uom.java.xmi.diff.MoveSourceFolderRefactoring;
import gr.uom.java.xmi.diff.RenameAttributeRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.RenamePackageRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.SplitAttributeRefactoring;
import gr.uom.java.xmi.diff.SplitVariableRefactoring;

public class RefactoringUtils {
	// Describe the level (type) of the refactoring
	public enum Level {
		// used to push the enum to int conversion to +1
		NONE, CLASS, METHOD, VARIABLE, ATTRIBUTE,
		// The refactoring did not fit any other level, e.g. Move Source Folder or
		// Rename Package refactorings
		OTHER
	}

	private static final Set<RefactoringType> classLevelRefactorings = Set.of(EXTRACT_INTERFACE, MOVE_CLASS,
			RENAME_CLASS, EXTRACT_CLASS, EXTRACT_SUBCLASS, EXTRACT_SUPERCLASS, MOVE_RENAME_CLASS,
			CONVERT_ANONYMOUS_CLASS_TO_TYPE, INTRODUCE_POLYMORPHISM);

	private static final Set<RefactoringType> variableLevelRefactorings = Set.of(CHANGE_VARIABLE_TYPE, SPLIT_VARIABLE,
			EXTRACT_VARIABLE, INLINE_VARIABLE, PARAMETERIZE_VARIABLE, RENAME_VARIABLE, REPLACE_VARIABLE_WITH_ATTRIBUTE,
			RENAME_PARAMETER, MERGE_VARIABLE);

	private static final Set<RefactoringType> attributeLevelRefactorings = Set.of(MOVE_ATTRIBUTE, PULL_UP_ATTRIBUTE,
			MOVE_RENAME_ATTRIBUTE, PUSH_DOWN_ATTRIBUTE, REPLACE_ATTRIBUTE, RENAME_ATTRIBUTE, EXTRACT_ATTRIBUTE,
			CHANGE_ATTRIBUTE_TYPE);

	private static final Set<RefactoringType> otherRefactorings = Set.of(MOVE_SOURCE_FOLDER, RENAME_PACKAGE);

	// note that we do not have 'change method signature' and 'merge operation' here
	// there's no detection for merge operation (i suppose it's still under
	// development)
	// there's also no change method signature refactoring (this is a
	// 'relationship', i need to understand it better)

	private static final Set<RefactoringType> methodLevelRefactorings = Set.of(CHANGE_RETURN_TYPE, RENAME_METHOD,
			MOVE_OPERATION, EXTRACT_AND_MOVE_OPERATION, EXTRACT_OPERATION, PULL_UP_OPERATION, PUSH_DOWN_OPERATION,
			INLINE_OPERATION, MOVE_AND_INLINE_OPERATION, MOVE_AND_RENAME_OPERATION, CHANGE_PARAMETER_TYPE,
			SPLIT_PARAMETER, MERGE_PARAMETER);

	public static boolean isAttributeLevelRefactoring(Refactoring refactoring) {
		return attributeLevelRefactorings.contains(refactoring.getRefactoringType());
	}

	public static boolean isMethodLevelRefactoring(Refactoring refactoring) {
		return methodLevelRefactorings.contains(refactoring.getRefactoringType());
	}

	public static boolean isClassLevelRefactoring(Refactoring refactoring) {
		return classLevelRefactorings.contains(refactoring.getRefactoringType());
	}

	public static boolean isVariableLevelRefactoring(Refactoring refactoring) {
		return variableLevelRefactorings.contains(refactoring.getRefactoringType());
	}

	public static boolean isOtherRefactoring(Refactoring refactoring) {
		return otherRefactorings.contains(refactoring.getRefactoringType());
	}

	public static int calculateLinesAdded(List<Edit> editList) {
		int linesAdded = 0;
		for (Edit edit : editList) {
			linesAdded += edit.getLengthB();
		}
		return linesAdded;
	}

	public static int calculateLinesDeleted(List<Edit> editList) {
		int linesDeleted = 0;
		for (Edit edit : editList) {
			linesDeleted += edit.getLengthA();
		}
		return linesDeleted;
	}

	// TODO: maybe in here we can find a way to add the full qualified names of
	// types
	// one needs to explore this 'UMLOperation' object a bit more
	public static String fullMethodName(UMLOperation operation) {

		String methodName = operation.getName();
		List<UMLType> parameters = operation.getParameterTypeList();

		int parameterCount = parameters.size();
		List<String> parameterTypes = new ArrayList<>();
		parameters.forEach(param -> {
			StringBuilder type = new StringBuilder(param.getClassType());
			type.append("[]".repeat(Math.max(0, param.getArrayDimension())));

			parameterTypes.add(type.toString());
		});

		return String.format("%s/%d%s%s%s", methodName, parameterCount, (parameterCount > 0 ? "[" : ""),
				(parameterCount > 0 ? String.join(",", parameterTypes) : ""), (parameterCount > 0 ? "]" : ""));
	}

	public static UMLOperation getRefactoredMethod(Refactoring refactoring) {
		if (refactoring instanceof RenameOperationRefactoring) {
			RenameOperationRefactoring convertedRefactoring = (RenameOperationRefactoring) refactoring;
			return convertedRefactoring.getOriginalOperation();
		}

		/*
		 * Is superclass for: PullUpOperationRefactoring PushDownOperationRefactoring
		 * ExtractAndMoveRefactoring
		 */
		if (refactoring instanceof MoveOperationRefactoring) {
			MoveOperationRefactoring convertedRefactoring = (MoveOperationRefactoring) refactoring;
			return convertedRefactoring.getOriginalOperation();
		}

		if (refactoring instanceof ExtractOperationRefactoring) {
			ExtractOperationRefactoring convertedRefactoring = (ExtractOperationRefactoring) refactoring;
			return convertedRefactoring.getSourceOperationBeforeExtraction();
		}

		if (refactoring instanceof InlineOperationRefactoring) {
			InlineOperationRefactoring convertedRefactoring = (InlineOperationRefactoring) refactoring;
			return convertedRefactoring.getInlinedOperation();
		}

		// now, if it's a variable refactoring, it happens inside of a method, which we
		// get it
		if (refactoring instanceof ExtractVariableRefactoring) {
			ExtractVariableRefactoring convertedRefactoring = (ExtractVariableRefactoring) refactoring;
			return convertedRefactoring.getOperationBefore();
		}

		if (refactoring instanceof InlineVariableRefactoring) {
			InlineVariableRefactoring convertedRefactoring = (InlineVariableRefactoring) refactoring;
			return convertedRefactoring.getOperationBefore();
		}

		if (refactoring instanceof RenameVariableRefactoring) {
			RenameVariableRefactoring convertedRefactoring = (RenameVariableRefactoring) refactoring;
			return convertedRefactoring.getOperationBefore();
		}

		if (refactoring instanceof ChangeReturnTypeRefactoring) {
			ChangeReturnTypeRefactoring convertedRefactoring = (ChangeReturnTypeRefactoring) refactoring;
			return convertedRefactoring.getOperationBefore();
		}

		// Also used by for variable refactorings
		if (refactoring instanceof ChangeVariableTypeRefactoring) {
			ChangeVariableTypeRefactoring convertedRefactoring = (ChangeVariableTypeRefactoring) refactoring;
			return convertedRefactoring.getOperationBefore();
		}

		// Also used by for variable refactorings
		if (refactoring instanceof SplitVariableRefactoring) {
			SplitVariableRefactoring convertedRefactoring = (SplitVariableRefactoring) refactoring;
			return convertedRefactoring.getOperationBefore();
		}

		// Also used by for variable refactorings
		if (refactoring instanceof MergeVariableRefactoring) {
			MergeVariableRefactoring convertedRefactoring = (MergeVariableRefactoring) refactoring;
			return convertedRefactoring.getOperationBefore();
		}

		throw new MethodRefactorMethodNotFoundException(refactoring);
	}

	public static class MethodRefactorMethodNotFoundException extends RuntimeException {

		/**
		 *
		 */
		private static final long serialVersionUID = -7405473357420788330L;

		/**
		 * @param message
		 */
		public MethodRefactorMethodNotFoundException(Refactoring refactoring) {
			super("This is a method-level refactoring, but it seems I can't get the refactored method: "
					+ refactoring.getRefactoringType());
		}

	}

	public static String getRefactoredVariableOrAttribute(Refactoring refactoring) {
		/*
		 * MoveAttributeRefactoring is super class for:
		 * MoveAndRenameAttributeRefactoring ReplaceAttributeRefactoring
		 * PullUpAttributeRefactoring PushDownAttributeRefactoring
		 */
		if (refactoring instanceof MoveAttributeRefactoring) {
			MoveAttributeRefactoring convertedRefactoring = (MoveAttributeRefactoring) refactoring;
			return convertedRefactoring.getOriginalAttribute().getName();
		}

		if (refactoring instanceof ExtractVariableRefactoring) {
			ExtractVariableRefactoring convertedRefactoring = (ExtractVariableRefactoring) refactoring;
			return convertedRefactoring.getVariableDeclaration().getVariableName();
		}

		if (refactoring instanceof InlineVariableRefactoring) {
			InlineVariableRefactoring convertedRefactoring = (InlineVariableRefactoring) refactoring;
			return convertedRefactoring.getVariableDeclaration().getVariableName();
		}

		if (refactoring instanceof RenameAttributeRefactoring) {
			RenameAttributeRefactoring convertedRefactoring = (RenameAttributeRefactoring) refactoring;
			return convertedRefactoring.getOriginalAttribute().getVariableDeclaration().getVariableName();
		}

		if (refactoring instanceof RenameVariableRefactoring) {
			RenameVariableRefactoring convertedRefactoring = (RenameVariableRefactoring) refactoring;
			return convertedRefactoring.getOriginalVariable().getVariableName();
		}

		// Also used by CHANGE_PARAMETER_TYPE
		if (refactoring instanceof ChangeVariableTypeRefactoring) {
			ChangeVariableTypeRefactoring convertedRefactoring = (ChangeVariableTypeRefactoring) refactoring;
			return convertedRefactoring.getOriginalVariable().getVariableName();
		}

		// Also used by SPLIT_PARAMETER
		if (refactoring instanceof SplitVariableRefactoring) {
			SplitVariableRefactoring convertedRefactoring = (SplitVariableRefactoring) refactoring;
			return convertedRefactoring.getOldVariable().getVariableName();
		}

		// Also used by MERGE_PARAMETER
		if (refactoring instanceof MergeVariableRefactoring) {
			MergeVariableRefactoring convertedRefactoring = (MergeVariableRefactoring) refactoring;
			return convertedRefactoring.getMergedVariables().stream().map(VariableDeclaration::getVariableName)
					.collect(Collectors.toList()).toString();
		}

		if (refactoring instanceof MergeAttributeRefactoring) {
			MergeAttributeRefactoring convertedRefactoring = (MergeAttributeRefactoring) refactoring;
			return convertedRefactoring.getMergedAttributes().stream().map(UMLAttribute::getVariableDeclaration)
					.map(VariableDeclaration::getVariableName).collect(Collectors.toList()).toString();
		}

		if (refactoring instanceof ExtractAttributeRefactoring) {
			ExtractAttributeRefactoring convertedRefactoring = (ExtractAttributeRefactoring) refactoring;
			return convertedRefactoring.getVariableDeclaration().getName();
		}

		if (refactoring instanceof SplitAttributeRefactoring) {
			SplitAttributeRefactoring convertedRefactoring = (SplitAttributeRefactoring) refactoring;
			return convertedRefactoring.getOldAttribute().getVariableDeclaration().getVariableName();
		}

		if (refactoring instanceof ChangeAttributeTypeRefactoring) {
			ChangeAttributeTypeRefactoring convertedRefactoring = (ChangeAttributeTypeRefactoring) refactoring;
			return convertedRefactoring.getChangedTypeAttribute().getVariableDeclaration().getVariableName();
		}

		throw new RuntimeException(
				"This is a variable-level refactoring, but it seems I can't get the refactored variable");
	}

	public static String cleanMethodName(String methodName) {
		return methodName.contains("/") ? methodName.substring(0, methodName.indexOf("/")) : methodName;
	}

	public static int refactoringTypeInNumber(Refactoring refactoring) {
		if (isClassLevelRefactoring(refactoring))
			return Level.CLASS.ordinal();
		if (isMethodLevelRefactoring(refactoring))
			return Level.METHOD.ordinal();
		if (isVariableLevelRefactoring(refactoring))
			return Level.VARIABLE.ordinal();
		if (isAttributeLevelRefactoring(refactoring))
			return Level.ATTRIBUTE.ordinal();
		if (isOtherRefactoring(refactoring))
			return Level.OTHER.ordinal();

		return -1;
	}

	public static boolean isStudied(Refactoring refactoring) {
		return refactoringTypeInNumber(refactoring) >= 0;
	}

	public static Set<ImmutablePair<String, String>> refactoredFilesAndClasses(Refactoring refactoring,
			Set<ImmutablePair<String, String>> classes) {
		/**
		 * if only one class is the origin of the refactoring (like most of them), just
		 * return the current list
		 */
		if (classes.size() == 1)
			return classes;

		/**
		 * if it's one of the refactorings below, this is a class-level refactoring so
		 * let's return the list with all of classes, as we want data points for each of
		 * these classes
		 */
		boolean extractSuperClassOrInterface = refactoring instanceof ExtractSuperclassRefactoring;
		boolean moveSourceFolder = refactoring instanceof MoveSourceFolderRefactoring;
		boolean renamePackageFolder = refactoring instanceof RenamePackageRefactoring;
		if (extractSuperClassOrInterface || moveSourceFolder || renamePackageFolder)
			return classes;

		/**
		 * if we get to here, we have in our hands a refactoring like Move and Inline,
		 * which might have more than a single class as origin for the refactoring.
		 *
		 * We, thus, would have to parse the refactoring description. However, RMiner's
		 * implementation currently use a LinkedHashSet<>, which keeps the order of
		 * insertion. In its implementation, the first element in the list is always the
		 * origin. For now, we hope this won't change in RMiner.
		 */
		Iterator<ImmutablePair<String, String>> it = classes.iterator();
		return Sets.newHashSet(it.next());
	}

	/**
	 * Get a map that contains classes that were renamed in this commit.
	 *
	 * Note that the map is name after -> name before. This is due to the fact that
	 * RMiner sometimes returns, for other refactorings, "the name before" = "the
	 * name after renaming".
	 */
	public static Map<String, String> getClassAliases(List<Refactoring> refactoringsToProcess) {
		Map<String, String> aliases = new HashMap<>();

		for (Refactoring rename : possibleClassRenames(refactoringsToProcess)) {

			String nameBefore = rename.getInvolvedClassesBeforeRefactoring().iterator().next().getRight();
			String nameAfter = rename.getInvolvedClassesAfterRefactoring().iterator().next().getRight();

			aliases.put(nameAfter, nameBefore);
		}

		return aliases;
	}

	public static boolean isClassRename(Refactoring refactoring) {
		List<RefactoringType> renameTypes = Arrays.asList(RefactoringType.MOVE_RENAME_CLASS,
				RefactoringType.RENAME_CLASS, RefactoringType.MOVE_CLASS);
		return renameTypes.contains(refactoring.getRefactoringType());
	}

	/**
	 * Return on the refactorings that might change the name of the class.
	 */
	public static List<Refactoring> possibleClassRenames(List<Refactoring> refactorings) {
		return refactorings.stream().filter(RefactoringUtils::isClassRename).collect(Collectors.toList());
	}

	/**
	 * We detect anonymous classes by means of a simple heuristic: if the last part
	 * of a full class name starts with a lower case, odds are that it's an
	 * anonymous class.
	 */
	public static boolean isAnonymousClass(String fullName) {
		String[] parts = fullName.split("\\.");
		return Character.isLowerCase(parts[parts.length - 1].charAt(0));

	}
}