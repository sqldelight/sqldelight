package org.antlr.intellij.adaptor.lexer;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.antlr.v4.runtime.Token;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ElementTypeFactory {
	private static final Map<Language, List<TokenElementType>> tokenElementTypesCache =
			new HashMap<Language, List<TokenElementType>>();
	private static final Map<Language, List<RuleElementType>> ruleElementTypesCache =
			new HashMap<Language, List<RuleElementType>>();
	private static final Map<Language, TokenElementType> eofElementTypesCache =
			new HashMap<Language, TokenElementType>();

	private ElementTypeFactory() {
	}

	public static TokenElementType getEofElementType(Language language) {
		TokenElementType result = eofElementTypesCache.get(language);
		if (result == null) {
			result = new TokenElementType(Token.EOF, "EOF", language);
			eofElementTypesCache.put(language, result);
		}

		return result;
	}

	public static List<TokenElementType> getTokenElementTypes(Language language, List<String> tokenNames) {
		List<TokenElementType> result = tokenElementTypesCache.get(language);
		if (result == null) {
			TokenElementType[] elementTypes = new TokenElementType[tokenNames.size()];
			for (int i = 0; i < tokenNames.size(); i++) {
				if ( tokenNames.get(i)!=null ) {
					elementTypes[i] = new TokenElementType(i, tokenNames.get(i), language);
				}
			}

			result = Collections.unmodifiableList(Arrays.asList(elementTypes));
			tokenElementTypesCache.put(language, result);
		}

		return result;
	}

	public synchronized static List<RuleElementType> getRuleElementTypes(Language language, List<String> ruleNames) {
		List<RuleElementType> result = ruleElementTypesCache.get(language);
		if (result == null) {
			RuleElementType[] elementTypes = new RuleElementType[ruleNames.size()];
			for (int i = 0; i < ruleNames.size(); i++) {
				elementTypes[i] = new RuleElementType(i, ruleNames.get(i), language);
			}

			result = Collections.unmodifiableList(Arrays.asList(elementTypes));
			ruleElementTypesCache.put(language, result);
		}

		return result;
	}

	public static TokenSet createTokenSet(Language language, List<String> tokenNames, int... types) {
		List<TokenElementType> tokenElementTypes = getTokenElementTypes(language, tokenNames);

		IElementType[] elementTypes = new IElementType[types.length];
		for (int i = 0; i < types.length; i++) {
			if (types[i] == Token.EOF) {
				elementTypes[i] = getEofElementType(language);
			}
			else {
				elementTypes[i] = tokenElementTypes.get(types[i]);
			}
		}

		return TokenSet.create(elementTypes);
	}

	public static TokenSet createRuleSet(Language language, List<String> ruleNames, int... rules) {
		List<RuleElementType> tokenElementTypes = getRuleElementTypes(language, ruleNames);

		IElementType[] elementTypes = new IElementType[rules.length];
		for (int i = 0; i < rules.length; i++) {
			if (rules[i] == Token.EOF) {
				elementTypes[i] = getEofElementType(language);
			}
			else {
				elementTypes[i] = tokenElementTypes.get(rules[i]);
			}
		}

		return TokenSet.create(elementTypes);
	}
}
