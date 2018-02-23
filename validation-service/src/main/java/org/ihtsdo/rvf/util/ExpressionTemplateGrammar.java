package org.ihtsdo.rvf.util;

import apg.Grammar;
import java.io.PrintStream;

public class ExpressionTemplateGrammar extends Grammar{

    // public API
    public static Grammar getInstance(){
        if(factoryInstance == null){
            factoryInstance = new ExpressionTemplateGrammar(getRules(), getUdts(), getOpcodes());
        }
        return factoryInstance;
    }

    // rule name enum
    public static int ruleCount = 115;
    public enum RuleNames{
        ANCESTOROF("ancestorOf", 83, 570, 1),
        ANCESTORORSELFOF("ancestorOrSelfOf", 84, 571, 1),
        ANYNONESCAPEDCHAR("anyNonEscapedChar", 32, 167, 10),
        ATTRIBUTE("attribute", 12, 90, 10),
        ATTRIBUTEGROUP("attributeGroup", 10, 72, 10),
        ATTRIBUTENAME("attributeName", 13, 100, 1),
        ATTRIBUTESET("attributeSet", 11, 82, 8),
        ATTRIBUTEVALUE("attributeValue", 14, 101, 10),
        BS("BS", 27, 157, 1),
        CARDINALITY("cardinality", 99, 707, 4),
        CHILDOF("childOf", 82, 569, 1),
        COMMENT("comment", 111, 745, 7),
        COMPOUNDEXPRESSIONCONSTRAINT("compoundExpressionConstraint", 67, 491, 4),
        CONCEPTID("conceptId", 7, 52, 1),
        CONCEPTREFERENCE("conceptReference", 6, 39, 13),
        CONCEPTREPLACEMENTSLOT("conceptReplacementSlot", 40, 228, 20),
        CONCRETEVALUEREPLACEMENTSLOT("concreteValueReplacementSlot", 43, 290, 4),
        CONJUNCTION("conjunction", 86, 573, 13),
        CONJUNCTIONATTRIBUTESET("conjunctionAttributeSet", 94, 646, 6),
        CONJUNCTIONEXPRESSIONCONSTRAINT("conjunctionExpressionConstraint", 68, 495, 8),
        CONJUNCTIONREFINEMENTSET("conjunctionRefinementSet", 90, 618, 6),
        CONSTRAINTOPERATOR("constraintOperator", 79, 560, 7),
        CR("CR", 24, 154, 1),
        DECIMALREPLACEMENTSLOT("decimalReplacementSlot", 46, 334, 20),
        DECIMALVALUE("decimalValue", 19, 137, 5),
        DEFINITIONSTATUS("definitionStatus", 2, 18, 3),
        DESCENDANTOF("descendantOf", 80, 567, 1),
        DESCENDANTORSELFOF("descendantOrSelfOf", 81, 568, 1),
        DIGIT("digit", 28, 158, 1),
        DIGITNONZERO("digitNonZero", 30, 160, 1),
        DISJUNCTION("disjunction", 87, 586, 8),
        DISJUNCTIONATTRIBUTESET("disjunctionAttributeSet", 95, 652, 6),
        DISJUNCTIONEXPRESSIONCONSTRAINT("disjunctionExpressionConstraint", 69, 503, 8),
        DISJUNCTIONREFINEMENTSET("disjunctionRefinementSet", 91, 624, 6),
        DOT("dot", 75, 547, 1),
        DOTTEDEXPRESSIONATTRIBUTE("dottedExpressionAttribute", 72, 523, 4),
        DOTTEDEXPRESSIONCONSTRAINT("dottedExpressionConstraint", 71, 517, 6),
        ECLATTRIBUTE("eclAttribute", 98, 678, 29),
        ECLATTRIBUTEGROUP("eclAttributeGroup", 97, 666, 12),
        ECLATTRIBUTENAME("eclAttributeName", 105, 718, 1),
        ECLATTRIBUTESET("eclAttributeSet", 93, 639, 7),
        ECLCONCEPTREFERENCE("eclConceptReference", 77, 549, 10),
        ECLFOCUSCONCEPT("eclFocusConcept", 74, 544, 3),
        ECLREFINEMENT("eclRefinement", 89, 611, 7),
        EQUIVALENTTO("equivalentTo", 3, 21, 1),
        ESCAPEDCHAR("escapedChar", 33, 177, 7),
        EXCLUSION("exclusion", 88, 594, 17),
        EXCLUSIONEXPRESSIONCONSTRAINT("exclusionExpressionConstraint", 70, 511, 6),
        EXCLUSIVEMAXIMUM("exclusiveMaximum", 58, 433, 1),
        EXCLUSIVEMINIMUM("exclusiveMinimum", 57, 432, 1),
        EXPRESSIONCOMPARISONOPERATOR("expressionComparisonOperator", 106, 719, 3),
        EXPRESSIONCONSTRAINT("expressionConstraint", 65, 477, 8),
        EXPRESSIONREPLACEMENTSLOT("expressionReplacementSlot", 41, 248, 22),
        EXPRESSIONTEMPLATE("expressionTemplate", 0, 0, 10),
        EXPRESSIONVALUE("expressionValue", 15, 111, 8),
        FOCUSCONCEPT("focusConcept", 5, 23, 16),
        HTAB("HTAB", 23, 153, 1),
        INTEGERREPLACEMENTSLOT("integerReplacementSlot", 45, 314, 20),
        INTEGERVALUE("integerValue", 18, 131, 6),
        LF("LF", 25, 155, 1),
        MANY("many", 103, 716, 1),
        MAXVALUE("maxValue", 102, 713, 3),
        MEMBEROF("memberOf", 76, 548, 1),
        MINVALUE("minValue", 100, 711, 1),
        MWS("mws", 110, 738, 7),
        NONFSLASH("nonFSlash", 114, 765, 10),
        NONNEGATIVEINTEGERVALUE("nonNegativeIntegerValue", 109, 732, 6),
        NONQUOTESTRINGVALUE("nonQuoteStringValue", 62, 454, 8),
        NONSTARCHAR("nonStarChar", 112, 752, 10),
        NONWSNONPIPE("nonwsNonPipe", 31, 161, 6),
        NUMERICCOMPARISONOPERATOR("numericComparisonOperator", 107, 722, 7),
        NUMERICVALUE("numericValue", 17, 123, 8),
        PARENTOF("parentOf", 85, 572, 1),
        QM("QM", 26, 156, 1),
        REFINEDEXPRESSIONCONSTRAINT("refinedExpressionConstraint", 66, 485, 6),
        REFINEMENT("refinement", 9, 60, 12),
        REVERSEFLAG("reverseFlag", 104, 717, 1),
        SCTID("sctId", 20, 142, 4),
        SLOTDECIMALMAXIMUM("slotDecimalMaximum", 56, 427, 5),
        SLOTDECIMALMINIMUM("slotDecimalMinimum", 55, 422, 5),
        SLOTDECIMALRANGE("slotDecimalRange", 54, 413, 9),
        SLOTDECIMALSET("slotDecimalSet", 50, 380, 14),
        SLOTINFORMATION("slotInformation", 64, 468, 9),
        SLOTINTEGERMAXIMUM("slotIntegerMaximum", 53, 408, 5),
        SLOTINTEGERMINIMUM("slotIntegerMinimum", 52, 403, 5),
        SLOTINTEGERRANGE("slotIntegerRange", 51, 394, 9),
        SLOTINTEGERSET("slotIntegerSet", 49, 366, 14),
        SLOTNAME("slotName", 59, 434, 5),
        SLOTSTRING("slotString", 61, 450, 4),
        SLOTSTRINGSET("slotStringSet", 48, 360, 6),
        SLOTTOKEN("slotToken", 60, 439, 11),
        SLOTTOKENSET("slotTokenSet", 47, 354, 6),
        SP("SP", 22, 152, 1),
        STARWITHNONFSLASH("starWithNonFSlash", 113, 762, 3),
        STRINGCOMPARISONOPERATOR("stringComparisonOperator", 108, 729, 3),
        STRINGREPLACEMENTSLOT("stringReplacementSlot", 44, 294, 20),
        STRINGVALUE("stringValue", 16, 119, 4),
        SUBATTRIBUTESET("subAttributeSet", 96, 658, 8),
        SUBEXPRESSION("subExpression", 1, 10, 8),
        SUBEXPRESSIONCONSTRAINT("subExpressionConstraint", 73, 527, 17),
        SUBREFINEMENT("subRefinement", 92, 630, 9),
        SUBTYPEOF("subtypeOf", 4, 22, 1),
        TEMPLATEINFORMATIONSLOT("templateInformationSlot", 63, 462, 6),
        TEMPLATEREPLACEMENTSLOT("templateReplacementSlot", 39, 223, 5),
        TEMPLATESLOT("templateSlot", 38, 220, 3),
        TERM("term", 8, 53, 7),
        TO("to", 101, 712, 1),
        TOKENREPLACEMENTSLOT("tokenReplacementSlot", 42, 270, 20),
        UTF8_2("UTF8-2", 34, 184, 3),
        UTF8_3("UTF8-3", 35, 187, 17),
        UTF8_4("UTF8-4", 36, 204, 15),
        UTF8_TAIL("UTF8-tail", 37, 219, 1),
        WILDCARD("wildCard", 78, 559, 1),
        WS("ws", 21, 146, 6),
        ZERO("zero", 29, 159, 1);
        private String name;
        private int id;
        private int offset;
        private int count;
        RuleNames(String string, int id, int offset, int count){
            this.name = string;
            this.id = id;
            this.offset = offset;
            this.count = count;
        }
        public  String ruleName(){return name;}
        public  int    ruleID(){return id;}
        private int    opcodeOffset(){return offset;}
        private int    opcodeCount(){return count;}
    }

    // UDT name enum
    public static int udtCount = 0;
    public enum UdtNames{
    }

    // private
    private static ExpressionTemplateGrammar factoryInstance = null;
    private ExpressionTemplateGrammar(Rule[] rules, Udt[] udts, Opcode[] opcodes){
        super(rules, udts, opcodes);
    }

    private static Rule[] getRules(){
    	Rule[] rules = new Rule[115];
        for(RuleNames r : RuleNames.values()){
            rules[r.ruleID()] = getRule(r.ruleID(), r.ruleName(), r.opcodeOffset(), r.opcodeCount());
        }
        return rules;
    }

    private static Udt[] getUdts(){
    	Udt[] udts = new Udt[0];
        return udts;
    }

        // opcodes
    private static Opcode[] getOpcodes(){
    	Opcode[] op = new Opcode[775];
        {int[] a = {1,2,8,9}; op[0] = getOpcodeCat(a);}
        op[1] = getOpcodeRnm(21, 146); // ws
        op[2] = getOpcodeRep((char)0, (char)1, 3);
        {int[] a = {4,7}; op[3] = getOpcodeCat(a);}
        {int[] a = {5,6}; op[4] = getOpcodeAlt(a);}
        op[5] = getOpcodeRnm(2, 18); // definitionStatus
        op[6] = getOpcodeRnm(42, 270); // tokenReplacementSlot
        op[7] = getOpcodeRnm(21, 146); // ws
        op[8] = getOpcodeRnm(1, 10); // subExpression
        op[9] = getOpcodeRnm(21, 146); // ws
        {int[] a = {11,12}; op[10] = getOpcodeCat(a);}
        op[11] = getOpcodeRnm(5, 23); // focusConcept
        op[12] = getOpcodeRep((char)0, (char)1, 13);
        {int[] a = {14,15,16,17}; op[13] = getOpcodeCat(a);}
        op[14] = getOpcodeRnm(21, 146); // ws
        {char[] a = {58}; op[15] = getOpcodeTls(a);}
        op[16] = getOpcodeRnm(21, 146); // ws
        op[17] = getOpcodeRnm(9, 60); // refinement
        {int[] a = {19,20}; op[18] = getOpcodeAlt(a);}
        op[19] = getOpcodeRnm(3, 21); // equivalentTo
        op[20] = getOpcodeRnm(4, 22); // subtypeOf
        {char[] a = {61,61,61}; op[21] = getOpcodeTls(a);}
        {char[] a = {60,60,60}; op[22] = getOpcodeTls(a);}
        {int[] a = {24,28,29}; op[23] = getOpcodeCat(a);}
        op[24] = getOpcodeRep((char)0, (char)1, 25);
        {int[] a = {26,27}; op[25] = getOpcodeCat(a);}
        op[26] = getOpcodeRnm(63, 462); // templateInformationSlot
        op[27] = getOpcodeRnm(21, 146); // ws
        op[28] = getOpcodeRnm(6, 39); // conceptReference
        op[29] = getOpcodeRep((char)0, Character.MAX_VALUE, 30);
        {int[] a = {31,32,33,34,38}; op[30] = getOpcodeCat(a);}
        op[31] = getOpcodeRnm(21, 146); // ws
        {char[] a = {43}; op[32] = getOpcodeTls(a);}
        op[33] = getOpcodeRnm(21, 146); // ws
        op[34] = getOpcodeRep((char)0, (char)1, 35);
        {int[] a = {36,37}; op[35] = getOpcodeCat(a);}
        op[36] = getOpcodeRnm(63, 462); // templateInformationSlot
        op[37] = getOpcodeRnm(21, 146); // ws
        op[38] = getOpcodeRnm(6, 39); // conceptReference
        {int[] a = {40,41,42}; op[39] = getOpcodeAlt(a);}
        op[40] = getOpcodeRnm(40, 228); // conceptReplacementSlot
        op[41] = getOpcodeRnm(41, 248); // expressionReplacementSlot
        {int[] a = {43,44}; op[42] = getOpcodeCat(a);}
        op[43] = getOpcodeRnm(7, 52); // conceptId
        op[44] = getOpcodeRep((char)0, (char)1, 45);
        {int[] a = {46,47,48,49,50,51}; op[45] = getOpcodeCat(a);}
        op[46] = getOpcodeRnm(21, 146); // ws
        {char[] a = {124}; op[47] = getOpcodeTls(a);}
        op[48] = getOpcodeRnm(21, 146); // ws
        op[49] = getOpcodeRnm(8, 53); // term
        op[50] = getOpcodeRnm(21, 146); // ws
        {char[] a = {124}; op[51] = getOpcodeTls(a);}
        op[52] = getOpcodeRnm(20, 142); // sctId
        {int[] a = {54,55}; op[53] = getOpcodeCat(a);}
        op[54] = getOpcodeRnm(31, 161); // nonwsNonPipe
        op[55] = getOpcodeRep((char)0, Character.MAX_VALUE, 56);
        {int[] a = {57,59}; op[56] = getOpcodeCat(a);}
        op[57] = getOpcodeRep((char)0, Character.MAX_VALUE, 58);
        op[58] = getOpcodeRnm(22, 152); // SP
        op[59] = getOpcodeRnm(31, 161); // nonwsNonPipe
        {int[] a = {61,64}; op[60] = getOpcodeCat(a);}
        {int[] a = {62,63}; op[61] = getOpcodeAlt(a);}
        op[62] = getOpcodeRnm(11, 82); // attributeSet
        op[63] = getOpcodeRnm(10, 72); // attributeGroup
        op[64] = getOpcodeRep((char)0, Character.MAX_VALUE, 65);
        {int[] a = {66,67,71}; op[65] = getOpcodeCat(a);}
        op[66] = getOpcodeRnm(21, 146); // ws
        op[67] = getOpcodeRep((char)0, (char)1, 68);
        {int[] a = {69,70}; op[68] = getOpcodeCat(a);}
        {char[] a = {44}; op[69] = getOpcodeTls(a);}
        op[70] = getOpcodeRnm(21, 146); // ws
        op[71] = getOpcodeRnm(10, 72); // attributeGroup
        {int[] a = {73,77,78,79,80,81}; op[72] = getOpcodeCat(a);}
        op[73] = getOpcodeRep((char)0, (char)1, 74);
        {int[] a = {75,76}; op[74] = getOpcodeCat(a);}
        op[75] = getOpcodeRnm(63, 462); // templateInformationSlot
        op[76] = getOpcodeRnm(21, 146); // ws
        {char[] a = {123}; op[77] = getOpcodeTls(a);}
        op[78] = getOpcodeRnm(21, 146); // ws
        op[79] = getOpcodeRnm(11, 82); // attributeSet
        op[80] = getOpcodeRnm(21, 146); // ws
        {char[] a = {125}; op[81] = getOpcodeTls(a);}
        {int[] a = {83,84}; op[82] = getOpcodeCat(a);}
        op[83] = getOpcodeRnm(12, 90); // attribute
        op[84] = getOpcodeRep((char)0, Character.MAX_VALUE, 85);
        {int[] a = {86,87,88,89}; op[85] = getOpcodeCat(a);}
        op[86] = getOpcodeRnm(21, 146); // ws
        {char[] a = {44}; op[87] = getOpcodeTls(a);}
        op[88] = getOpcodeRnm(21, 146); // ws
        op[89] = getOpcodeRnm(12, 90); // attribute
        {int[] a = {91,95,96,97,98,99}; op[90] = getOpcodeCat(a);}
        op[91] = getOpcodeRep((char)0, (char)1, 92);
        {int[] a = {93,94}; op[92] = getOpcodeCat(a);}
        op[93] = getOpcodeRnm(63, 462); // templateInformationSlot
        op[94] = getOpcodeRnm(21, 146); // ws
        op[95] = getOpcodeRnm(13, 100); // attributeName
        op[96] = getOpcodeRnm(21, 146); // ws
        {char[] a = {61}; op[97] = getOpcodeTls(a);}
        op[98] = getOpcodeRnm(21, 146); // ws
        op[99] = getOpcodeRnm(14, 101); // attributeValue
        op[100] = getOpcodeRnm(6, 39); // conceptReference
        {int[] a = {102,103,107,110}; op[101] = getOpcodeAlt(a);}
        op[102] = getOpcodeRnm(15, 111); // expressionValue
        {int[] a = {104,105,106}; op[103] = getOpcodeCat(a);}
        op[104] = getOpcodeRnm(26, 156); // QM
        op[105] = getOpcodeRnm(16, 119); // stringValue
        op[106] = getOpcodeRnm(26, 156); // QM
        {int[] a = {108,109}; op[107] = getOpcodeCat(a);}
        {char[] a = {35}; op[108] = getOpcodeTls(a);}
        op[109] = getOpcodeRnm(17, 123); // numericValue
        op[110] = getOpcodeRnm(43, 290); // concreteValueReplacementSlot
        {int[] a = {112,113}; op[111] = getOpcodeAlt(a);}
        op[112] = getOpcodeRnm(6, 39); // conceptReference
        {int[] a = {114,115,116,117,118}; op[113] = getOpcodeCat(a);}
        {char[] a = {40}; op[114] = getOpcodeTls(a);}
        op[115] = getOpcodeRnm(21, 146); // ws
        op[116] = getOpcodeRnm(1, 10); // subExpression
        op[117] = getOpcodeRnm(21, 146); // ws
        {char[] a = {41}; op[118] = getOpcodeTls(a);}
        op[119] = getOpcodeRep((char)1, Character.MAX_VALUE, 120);
        {int[] a = {121,122}; op[120] = getOpcodeAlt(a);}
        op[121] = getOpcodeRnm(32, 167); // anyNonEscapedChar
        op[122] = getOpcodeRnm(33, 177); // escapedChar
        {int[] a = {124,128}; op[123] = getOpcodeCat(a);}
        op[124] = getOpcodeRep((char)0, (char)1, 125);
        {int[] a = {126,127}; op[125] = getOpcodeAlt(a);}
        {char[] a = {45}; op[126] = getOpcodeTls(a);}
        {char[] a = {43}; op[127] = getOpcodeTls(a);}
        {int[] a = {129,130}; op[128] = getOpcodeAlt(a);}
        op[129] = getOpcodeRnm(19, 137); // decimalValue
        op[130] = getOpcodeRnm(18, 131); // integerValue
        {int[] a = {132,136}; op[131] = getOpcodeAlt(a);}
        {int[] a = {133,134}; op[132] = getOpcodeCat(a);}
        op[133] = getOpcodeRnm(30, 160); // digitNonZero
        op[134] = getOpcodeRep((char)0, Character.MAX_VALUE, 135);
        op[135] = getOpcodeRnm(28, 158); // digit
        op[136] = getOpcodeRnm(29, 159); // zero
        {int[] a = {138,139,140}; op[137] = getOpcodeCat(a);}
        op[138] = getOpcodeRnm(18, 131); // integerValue
        {char[] a = {46}; op[139] = getOpcodeTls(a);}
        op[140] = getOpcodeRep((char)1, Character.MAX_VALUE, 141);
        op[141] = getOpcodeRnm(28, 158); // digit
        {int[] a = {143,144}; op[142] = getOpcodeCat(a);}
        op[143] = getOpcodeRnm(30, 160); // digitNonZero
        op[144] = getOpcodeRep((char)5, (char)17, 145);
        op[145] = getOpcodeRnm(28, 158); // digit
        op[146] = getOpcodeRep((char)0, Character.MAX_VALUE, 147);
        {int[] a = {148,149,150,151}; op[147] = getOpcodeAlt(a);}
        op[148] = getOpcodeRnm(22, 152); // SP
        op[149] = getOpcodeRnm(23, 153); // HTAB
        op[150] = getOpcodeRnm(24, 154); // CR
        op[151] = getOpcodeRnm(25, 155); // LF
        {char[] a = {32}; op[152] = getOpcodeTbs(a);}
        {char[] a = {9}; op[153] = getOpcodeTbs(a);}
        {char[] a = {13}; op[154] = getOpcodeTbs(a);}
        {char[] a = {10}; op[155] = getOpcodeTbs(a);}
        {char[] a = {34}; op[156] = getOpcodeTbs(a);}
        {char[] a = {92}; op[157] = getOpcodeTbs(a);}
        op[158] = getOpcodeTrg((char)48, (char)57);
        {char[] a = {48}; op[159] = getOpcodeTbs(a);}
        op[160] = getOpcodeTrg((char)49, (char)57);
        {int[] a = {162,163,164,165,166}; op[161] = getOpcodeAlt(a);}
        op[162] = getOpcodeTrg((char)33, (char)123);
        op[163] = getOpcodeTrg((char)125, (char)126);
        op[164] = getOpcodeRnm(34, 184); // UTF8-2
        op[165] = getOpcodeRnm(35, 187); // UTF8-3
        op[166] = getOpcodeRnm(36, 204); // UTF8-4
        {int[] a = {168,169,170,171,172,173,174,175,176}; op[167] = getOpcodeAlt(a);}
        op[168] = getOpcodeRnm(23, 153); // HTAB
        op[169] = getOpcodeRnm(24, 154); // CR
        op[170] = getOpcodeRnm(25, 155); // LF
        op[171] = getOpcodeTrg((char)32, (char)33);
        op[172] = getOpcodeTrg((char)35, (char)91);
        op[173] = getOpcodeTrg((char)93, (char)126);
        op[174] = getOpcodeRnm(34, 184); // UTF8-2
        op[175] = getOpcodeRnm(35, 187); // UTF8-3
        op[176] = getOpcodeRnm(36, 204); // UTF8-4
        {int[] a = {178,181}; op[177] = getOpcodeAlt(a);}
        {int[] a = {179,180}; op[178] = getOpcodeCat(a);}
        op[179] = getOpcodeRnm(27, 157); // BS
        op[180] = getOpcodeRnm(26, 156); // QM
        {int[] a = {182,183}; op[181] = getOpcodeCat(a);}
        op[182] = getOpcodeRnm(27, 157); // BS
        op[183] = getOpcodeRnm(27, 157); // BS
        {int[] a = {185,186}; op[184] = getOpcodeCat(a);}
        op[185] = getOpcodeTrg((char)194, (char)223);
        op[186] = getOpcodeRnm(37, 219); // UTF8-tail
        {int[] a = {188,192,196,200}; op[187] = getOpcodeAlt(a);}
        {int[] a = {189,190,191}; op[188] = getOpcodeCat(a);}
        {char[] a = {224}; op[189] = getOpcodeTbs(a);}
        op[190] = getOpcodeTrg((char)160, (char)191);
        op[191] = getOpcodeRnm(37, 219); // UTF8-tail
        {int[] a = {193,194}; op[192] = getOpcodeCat(a);}
        op[193] = getOpcodeTrg((char)225, (char)236);
        op[194] = getOpcodeRep((char)2, (char)2, 195);
        op[195] = getOpcodeRnm(37, 219); // UTF8-tail
        {int[] a = {197,198,199}; op[196] = getOpcodeCat(a);}
        {char[] a = {237}; op[197] = getOpcodeTbs(a);}
        op[198] = getOpcodeTrg((char)128, (char)159);
        op[199] = getOpcodeRnm(37, 219); // UTF8-tail
        {int[] a = {201,202}; op[200] = getOpcodeCat(a);}
        op[201] = getOpcodeTrg((char)238, (char)239);
        op[202] = getOpcodeRep((char)2, (char)2, 203);
        op[203] = getOpcodeRnm(37, 219); // UTF8-tail
        {int[] a = {205,210,214}; op[204] = getOpcodeAlt(a);}
        {int[] a = {206,207,208}; op[205] = getOpcodeCat(a);}
        {char[] a = {240}; op[206] = getOpcodeTbs(a);}
        op[207] = getOpcodeTrg((char)144, (char)191);
        op[208] = getOpcodeRep((char)2, (char)2, 209);
        op[209] = getOpcodeRnm(37, 219); // UTF8-tail
        {int[] a = {211,212}; op[210] = getOpcodeCat(a);}
        op[211] = getOpcodeTrg((char)241, (char)243);
        op[212] = getOpcodeRep((char)3, (char)3, 213);
        op[213] = getOpcodeRnm(37, 219); // UTF8-tail
        {int[] a = {215,216,217}; op[214] = getOpcodeCat(a);}
        {char[] a = {244}; op[215] = getOpcodeTbs(a);}
        op[216] = getOpcodeTrg((char)128, (char)143);
        op[217] = getOpcodeRep((char)2, (char)2, 218);
        op[218] = getOpcodeRnm(37, 219); // UTF8-tail
        op[219] = getOpcodeTrg((char)128, (char)191);
        {int[] a = {221,222}; op[220] = getOpcodeAlt(a);}
        op[221] = getOpcodeRnm(39, 223); // templateReplacementSlot
        op[222] = getOpcodeRnm(63, 462); // templateInformationSlot
        {int[] a = {224,225,226,227}; op[223] = getOpcodeAlt(a);}
        op[224] = getOpcodeRnm(40, 228); // conceptReplacementSlot
        op[225] = getOpcodeRnm(41, 248); // expressionReplacementSlot
        op[226] = getOpcodeRnm(42, 270); // tokenReplacementSlot
        op[227] = getOpcodeRnm(43, 290); // concreteValueReplacementSlot
        {int[] a = {229,230,231,232,233,234,235,243,247}; op[228] = getOpcodeCat(a);}
        {char[] a = {91,91}; op[229] = getOpcodeTls(a);}
        op[230] = getOpcodeRnm(21, 146); // ws
        {char[] a = {43}; op[231] = getOpcodeTls(a);}
        op[232] = getOpcodeRnm(21, 146); // ws
        {char[] a = {105,100}; op[233] = getOpcodeTls(a);}
        op[234] = getOpcodeRnm(21, 146); // ws
        op[235] = getOpcodeRep((char)0, (char)1, 236);
        {int[] a = {237,238,239,240,241,242}; op[236] = getOpcodeCat(a);}
        {char[] a = {40}; op[237] = getOpcodeTls(a);}
        op[238] = getOpcodeRnm(21, 146); // ws
        op[239] = getOpcodeRnm(65, 477); // expressionConstraint
        op[240] = getOpcodeRnm(21, 146); // ws
        {char[] a = {41}; op[241] = getOpcodeTls(a);}
        op[242] = getOpcodeRnm(21, 146); // ws
        op[243] = getOpcodeRep((char)0, (char)1, 244);
        {int[] a = {245,246}; op[244] = getOpcodeCat(a);}
        op[245] = getOpcodeRnm(59, 434); // slotName
        op[246] = getOpcodeRnm(21, 146); // ws
        {char[] a = {93,93}; op[247] = getOpcodeTls(a);}
        {int[] a = {249,250,251,252,253,257,265,269}; op[248] = getOpcodeCat(a);}
        {char[] a = {91,91}; op[249] = getOpcodeTls(a);}
        op[250] = getOpcodeRnm(21, 146); // ws
        {char[] a = {43}; op[251] = getOpcodeTls(a);}
        op[252] = getOpcodeRnm(21, 146); // ws
        op[253] = getOpcodeRep((char)0, (char)1, 254);
        {int[] a = {255,256}; op[254] = getOpcodeCat(a);}
        {char[] a = {115,99,103}; op[255] = getOpcodeTls(a);}
        op[256] = getOpcodeRnm(21, 146); // ws
        op[257] = getOpcodeRep((char)0, (char)1, 258);
        {int[] a = {259,260,261,262,263,264}; op[258] = getOpcodeCat(a);}
        {char[] a = {40}; op[259] = getOpcodeTls(a);}
        op[260] = getOpcodeRnm(21, 146); // ws
        op[261] = getOpcodeRnm(65, 477); // expressionConstraint
        op[262] = getOpcodeRnm(21, 146); // ws
        {char[] a = {41}; op[263] = getOpcodeTls(a);}
        op[264] = getOpcodeRnm(21, 146); // ws
        op[265] = getOpcodeRep((char)0, (char)1, 266);
        {int[] a = {267,268}; op[266] = getOpcodeCat(a);}
        op[267] = getOpcodeRnm(59, 434); // slotName
        op[268] = getOpcodeRnm(21, 146); // ws
        {char[] a = {93,93}; op[269] = getOpcodeTls(a);}
        {int[] a = {271,272,273,274,275,276,277,285,289}; op[270] = getOpcodeCat(a);}
        {char[] a = {91,91}; op[271] = getOpcodeTls(a);}
        op[272] = getOpcodeRnm(21, 146); // ws
        {char[] a = {43}; op[273] = getOpcodeTls(a);}
        op[274] = getOpcodeRnm(21, 146); // ws
        {char[] a = {116,111,107}; op[275] = getOpcodeTls(a);}
        op[276] = getOpcodeRnm(21, 146); // ws
        op[277] = getOpcodeRep((char)0, (char)1, 278);
        {int[] a = {279,280,281,282,283,284}; op[278] = getOpcodeCat(a);}
        {char[] a = {40}; op[279] = getOpcodeTls(a);}
        op[280] = getOpcodeRnm(21, 146); // ws
        op[281] = getOpcodeRnm(47, 354); // slotTokenSet
        op[282] = getOpcodeRnm(21, 146); // ws
        {char[] a = {41}; op[283] = getOpcodeTls(a);}
        op[284] = getOpcodeRnm(21, 146); // ws
        op[285] = getOpcodeRep((char)0, (char)1, 286);
        {int[] a = {287,288}; op[286] = getOpcodeCat(a);}
        op[287] = getOpcodeRnm(59, 434); // slotName
        op[288] = getOpcodeRnm(21, 146); // ws
        {char[] a = {93,93}; op[289] = getOpcodeTls(a);}
        {int[] a = {291,292,293}; op[290] = getOpcodeAlt(a);}
        op[291] = getOpcodeRnm(44, 294); // stringReplacementSlot
        op[292] = getOpcodeRnm(45, 314); // integerReplacementSlot
        op[293] = getOpcodeRnm(46, 334); // decimalReplacementSlot
        {int[] a = {295,296,297,298,299,300,301,309,313}; op[294] = getOpcodeCat(a);}
        {char[] a = {91,91}; op[295] = getOpcodeTls(a);}
        op[296] = getOpcodeRnm(21, 146); // ws
        {char[] a = {43}; op[297] = getOpcodeTls(a);}
        op[298] = getOpcodeRnm(21, 146); // ws
        {char[] a = {115,116,114}; op[299] = getOpcodeTls(a);}
        op[300] = getOpcodeRnm(21, 146); // ws
        op[301] = getOpcodeRep((char)0, (char)1, 302);
        {int[] a = {303,304,305,306,307,308}; op[302] = getOpcodeCat(a);}
        {char[] a = {40}; op[303] = getOpcodeTls(a);}
        op[304] = getOpcodeRnm(21, 146); // ws
        op[305] = getOpcodeRnm(48, 360); // slotStringSet
        op[306] = getOpcodeRnm(21, 146); // ws
        {char[] a = {41}; op[307] = getOpcodeTls(a);}
        op[308] = getOpcodeRnm(21, 146); // ws
        op[309] = getOpcodeRep((char)0, (char)1, 310);
        {int[] a = {311,312}; op[310] = getOpcodeCat(a);}
        op[311] = getOpcodeRnm(59, 434); // slotName
        op[312] = getOpcodeRnm(21, 146); // ws
        {char[] a = {93,93}; op[313] = getOpcodeTls(a);}
        {int[] a = {315,316,317,318,319,320,321,329,333}; op[314] = getOpcodeCat(a);}
        {char[] a = {91,91}; op[315] = getOpcodeTls(a);}
        op[316] = getOpcodeRnm(21, 146); // ws
        {char[] a = {43}; op[317] = getOpcodeTls(a);}
        op[318] = getOpcodeRnm(21, 146); // ws
        {char[] a = {105,110,116}; op[319] = getOpcodeTls(a);}
        op[320] = getOpcodeRnm(21, 146); // ws
        op[321] = getOpcodeRep((char)0, (char)1, 322);
        {int[] a = {323,324,325,326,327,328}; op[322] = getOpcodeCat(a);}
        {char[] a = {40}; op[323] = getOpcodeTls(a);}
        op[324] = getOpcodeRnm(21, 146); // ws
        op[325] = getOpcodeRnm(49, 366); // slotIntegerSet
        op[326] = getOpcodeRnm(21, 146); // ws
        {char[] a = {41}; op[327] = getOpcodeTls(a);}
        op[328] = getOpcodeRnm(21, 146); // ws
        op[329] = getOpcodeRep((char)0, (char)1, 330);
        {int[] a = {331,332}; op[330] = getOpcodeCat(a);}
        op[331] = getOpcodeRnm(59, 434); // slotName
        op[332] = getOpcodeRnm(21, 146); // ws
        {char[] a = {93,93}; op[333] = getOpcodeTls(a);}
        {int[] a = {335,336,337,338,339,340,341,349,353}; op[334] = getOpcodeCat(a);}
        {char[] a = {91,91}; op[335] = getOpcodeTls(a);}
        op[336] = getOpcodeRnm(21, 146); // ws
        {char[] a = {43}; op[337] = getOpcodeTls(a);}
        op[338] = getOpcodeRnm(21, 146); // ws
        {char[] a = {100,101,99}; op[339] = getOpcodeTls(a);}
        op[340] = getOpcodeRnm(21, 146); // ws
        op[341] = getOpcodeRep((char)0, (char)1, 342);
        {int[] a = {343,344,345,346,347,348}; op[342] = getOpcodeCat(a);}
        {char[] a = {40}; op[343] = getOpcodeTls(a);}
        op[344] = getOpcodeRnm(21, 146); // ws
        op[345] = getOpcodeRnm(50, 380); // slotDecimalSet
        op[346] = getOpcodeRnm(21, 146); // ws
        {char[] a = {41}; op[347] = getOpcodeTls(a);}
        op[348] = getOpcodeRnm(21, 146); // ws
        op[349] = getOpcodeRep((char)0, (char)1, 350);
        {int[] a = {351,352}; op[350] = getOpcodeCat(a);}
        op[351] = getOpcodeRnm(59, 434); // slotName
        op[352] = getOpcodeRnm(21, 146); // ws
        {char[] a = {93,93}; op[353] = getOpcodeTls(a);}
        {int[] a = {355,356}; op[354] = getOpcodeCat(a);}
        op[355] = getOpcodeRnm(60, 439); // slotToken
        op[356] = getOpcodeRep((char)0, Character.MAX_VALUE, 357);
        {int[] a = {358,359}; op[357] = getOpcodeCat(a);}
        op[358] = getOpcodeRnm(110, 738); // mws
        op[359] = getOpcodeRnm(60, 439); // slotToken
        {int[] a = {361,362}; op[360] = getOpcodeCat(a);}
        op[361] = getOpcodeRnm(61, 450); // slotString
        op[362] = getOpcodeRep((char)0, Character.MAX_VALUE, 363);
        {int[] a = {364,365}; op[363] = getOpcodeCat(a);}
        op[364] = getOpcodeRnm(110, 738); // mws
        op[365] = getOpcodeRnm(61, 450); // slotString
        {int[] a = {367,372}; op[366] = getOpcodeCat(a);}
        {int[] a = {368,371}; op[367] = getOpcodeAlt(a);}
        {int[] a = {369,370}; op[368] = getOpcodeCat(a);}
        {char[] a = {35}; op[369] = getOpcodeTls(a);}
        op[370] = getOpcodeRnm(18, 131); // integerValue
        op[371] = getOpcodeRnm(51, 394); // slotIntegerRange
        op[372] = getOpcodeRep((char)0, Character.MAX_VALUE, 373);
        {int[] a = {374,375}; op[373] = getOpcodeCat(a);}
        op[374] = getOpcodeRnm(110, 738); // mws
        {int[] a = {376,379}; op[375] = getOpcodeAlt(a);}
        {int[] a = {377,378}; op[376] = getOpcodeCat(a);}
        {char[] a = {35}; op[377] = getOpcodeTls(a);}
        op[378] = getOpcodeRnm(18, 131); // integerValue
        op[379] = getOpcodeRnm(51, 394); // slotIntegerRange
        {int[] a = {381,386}; op[380] = getOpcodeCat(a);}
        {int[] a = {382,385}; op[381] = getOpcodeAlt(a);}
        {int[] a = {383,384}; op[382] = getOpcodeCat(a);}
        {char[] a = {35}; op[383] = getOpcodeTls(a);}
        op[384] = getOpcodeRnm(19, 137); // decimalValue
        op[385] = getOpcodeRnm(54, 413); // slotDecimalRange
        op[386] = getOpcodeRep((char)0, Character.MAX_VALUE, 387);
        {int[] a = {388,389}; op[387] = getOpcodeCat(a);}
        op[388] = getOpcodeRnm(110, 738); // mws
        {int[] a = {390,393}; op[389] = getOpcodeAlt(a);}
        {int[] a = {391,392}; op[390] = getOpcodeCat(a);}
        {char[] a = {35}; op[391] = getOpcodeTls(a);}
        op[392] = getOpcodeRnm(19, 137); // decimalValue
        op[393] = getOpcodeRnm(54, 413); // slotDecimalRange
        {int[] a = {395,400}; op[394] = getOpcodeAlt(a);}
        {int[] a = {396,397,398}; op[395] = getOpcodeCat(a);}
        op[396] = getOpcodeRnm(52, 403); // slotIntegerMinimum
        op[397] = getOpcodeRnm(101, 712); // to
        op[398] = getOpcodeRep((char)0, (char)1, 399);
        op[399] = getOpcodeRnm(53, 408); // slotIntegerMaximum
        {int[] a = {401,402}; op[400] = getOpcodeCat(a);}
        op[401] = getOpcodeRnm(101, 712); // to
        op[402] = getOpcodeRnm(53, 408); // slotIntegerMaximum
        {int[] a = {404,406,407}; op[403] = getOpcodeCat(a);}
        op[404] = getOpcodeRep((char)0, (char)1, 405);
        op[405] = getOpcodeRnm(57, 432); // exclusiveMinimum
        {char[] a = {35}; op[406] = getOpcodeTls(a);}
        op[407] = getOpcodeRnm(18, 131); // integerValue
        {int[] a = {409,411,412}; op[408] = getOpcodeCat(a);}
        op[409] = getOpcodeRep((char)0, (char)1, 410);
        op[410] = getOpcodeRnm(58, 433); // exclusiveMaximum
        {char[] a = {35}; op[411] = getOpcodeTls(a);}
        op[412] = getOpcodeRnm(18, 131); // integerValue
        {int[] a = {414,419}; op[413] = getOpcodeAlt(a);}
        {int[] a = {415,416,417}; op[414] = getOpcodeCat(a);}
        op[415] = getOpcodeRnm(55, 422); // slotDecimalMinimum
        op[416] = getOpcodeRnm(101, 712); // to
        op[417] = getOpcodeRep((char)0, (char)1, 418);
        op[418] = getOpcodeRnm(56, 427); // slotDecimalMaximum
        {int[] a = {420,421}; op[419] = getOpcodeCat(a);}
        op[420] = getOpcodeRnm(101, 712); // to
        op[421] = getOpcodeRnm(56, 427); // slotDecimalMaximum
        {int[] a = {423,425,426}; op[422] = getOpcodeCat(a);}
        op[423] = getOpcodeRep((char)0, (char)1, 424);
        op[424] = getOpcodeRnm(57, 432); // exclusiveMinimum
        {char[] a = {35}; op[425] = getOpcodeTls(a);}
        op[426] = getOpcodeRnm(19, 137); // decimalValue
        {int[] a = {428,430,431}; op[427] = getOpcodeCat(a);}
        op[428] = getOpcodeRep((char)0, (char)1, 429);
        op[429] = getOpcodeRnm(58, 433); // exclusiveMaximum
        {char[] a = {35}; op[430] = getOpcodeTls(a);}
        op[431] = getOpcodeRnm(19, 137); // decimalValue
        {char[] a = {62}; op[432] = getOpcodeTls(a);}
        {char[] a = {60}; op[433] = getOpcodeTls(a);}
        {int[] a = {435,436}; op[434] = getOpcodeCat(a);}
        {char[] a = {64}; op[435] = getOpcodeTls(a);}
        {int[] a = {437,438}; op[436] = getOpcodeAlt(a);}
        op[437] = getOpcodeRnm(62, 454); // nonQuoteStringValue
        op[438] = getOpcodeRnm(61, 450); // slotString
        {int[] a = {440,441,442,443,444,445,446,447,448,449}; op[439] = getOpcodeAlt(a);}
        op[440] = getOpcodeRnm(2, 18); // definitionStatus
        op[441] = getOpcodeRnm(76, 548); // memberOf
        op[442] = getOpcodeRnm(79, 560); // constraintOperator
        op[443] = getOpcodeRnm(86, 573); // conjunction
        op[444] = getOpcodeRnm(87, 586); // disjunction
        op[445] = getOpcodeRnm(88, 594); // exclusion
        op[446] = getOpcodeRnm(104, 717); // reverseFlag
        op[447] = getOpcodeRnm(106, 719); // expressionComparisonOperator
        op[448] = getOpcodeRnm(107, 722); // numericComparisonOperator
        op[449] = getOpcodeRnm(108, 729); // stringComparisonOperator
        {int[] a = {451,452,453}; op[450] = getOpcodeCat(a);}
        op[451] = getOpcodeRnm(26, 156); // QM
        op[452] = getOpcodeRnm(16, 119); // stringValue
        op[453] = getOpcodeRnm(26, 156); // QM
        op[454] = getOpcodeRep((char)0, Character.MAX_VALUE, 455);
        {int[] a = {456,457,458,459,460,461}; op[455] = getOpcodeAlt(a);}
        {char[] a = {33}; op[456] = getOpcodeTbs(a);}
        op[457] = getOpcodeTrg((char)35, (char)38);
        op[458] = getOpcodeTrg((char)40, (char)63);
        op[459] = getOpcodeTrg((char)65, (char)90);
        {char[] a = {92}; op[460] = getOpcodeTbs(a);}
        op[461] = getOpcodeTrg((char)94, (char)126);
        {int[] a = {463,464,465,466,467}; op[462] = getOpcodeCat(a);}
        {char[] a = {91,91}; op[463] = getOpcodeTls(a);}
        op[464] = getOpcodeRnm(21, 146); // ws
        op[465] = getOpcodeRnm(64, 468); // slotInformation
        op[466] = getOpcodeRnm(21, 146); // ws
        {char[] a = {93,93}; op[467] = getOpcodeTls(a);}
        {int[] a = {469,473}; op[468] = getOpcodeCat(a);}
        op[469] = getOpcodeRep((char)0, (char)1, 470);
        {int[] a = {471,472}; op[470] = getOpcodeCat(a);}
        op[471] = getOpcodeRnm(99, 707); // cardinality
        op[472] = getOpcodeRnm(21, 146); // ws
        op[473] = getOpcodeRep((char)0, (char)1, 474);
        {int[] a = {475,476}; op[474] = getOpcodeCat(a);}
        op[475] = getOpcodeRnm(59, 434); // slotName
        op[476] = getOpcodeRnm(21, 146); // ws
        {int[] a = {478,479,484}; op[477] = getOpcodeCat(a);}
        op[478] = getOpcodeRnm(21, 146); // ws
        {int[] a = {480,481,482,483}; op[479] = getOpcodeAlt(a);}
        op[480] = getOpcodeRnm(66, 485); // refinedExpressionConstraint
        op[481] = getOpcodeRnm(67, 491); // compoundExpressionConstraint
        op[482] = getOpcodeRnm(71, 517); // dottedExpressionConstraint
        op[483] = getOpcodeRnm(73, 527); // subExpressionConstraint
        op[484] = getOpcodeRnm(21, 146); // ws
        {int[] a = {486,487,488,489,490}; op[485] = getOpcodeCat(a);}
        op[486] = getOpcodeRnm(73, 527); // subExpressionConstraint
        op[487] = getOpcodeRnm(21, 146); // ws
        {char[] a = {58}; op[488] = getOpcodeTls(a);}
        op[489] = getOpcodeRnm(21, 146); // ws
        op[490] = getOpcodeRnm(89, 611); // eclRefinement
        {int[] a = {492,493,494}; op[491] = getOpcodeAlt(a);}
        op[492] = getOpcodeRnm(68, 495); // conjunctionExpressionConstraint
        op[493] = getOpcodeRnm(69, 503); // disjunctionExpressionConstraint
        op[494] = getOpcodeRnm(70, 511); // exclusionExpressionConstraint
        {int[] a = {496,497}; op[495] = getOpcodeCat(a);}
        op[496] = getOpcodeRnm(73, 527); // subExpressionConstraint
        op[497] = getOpcodeRep((char)1, Character.MAX_VALUE, 498);
        {int[] a = {499,500,501,502}; op[498] = getOpcodeCat(a);}
        op[499] = getOpcodeRnm(21, 146); // ws
        op[500] = getOpcodeRnm(86, 573); // conjunction
        op[501] = getOpcodeRnm(21, 146); // ws
        op[502] = getOpcodeRnm(73, 527); // subExpressionConstraint
        {int[] a = {504,505}; op[503] = getOpcodeCat(a);}
        op[504] = getOpcodeRnm(73, 527); // subExpressionConstraint
        op[505] = getOpcodeRep((char)1, Character.MAX_VALUE, 506);
        {int[] a = {507,508,509,510}; op[506] = getOpcodeCat(a);}
        op[507] = getOpcodeRnm(21, 146); // ws
        op[508] = getOpcodeRnm(87, 586); // disjunction
        op[509] = getOpcodeRnm(21, 146); // ws
        op[510] = getOpcodeRnm(73, 527); // subExpressionConstraint
        {int[] a = {512,513,514,515,516}; op[511] = getOpcodeCat(a);}
        op[512] = getOpcodeRnm(73, 527); // subExpressionConstraint
        op[513] = getOpcodeRnm(21, 146); // ws
        op[514] = getOpcodeRnm(88, 594); // exclusion
        op[515] = getOpcodeRnm(21, 146); // ws
        op[516] = getOpcodeRnm(73, 527); // subExpressionConstraint
        {int[] a = {518,519}; op[517] = getOpcodeCat(a);}
        op[518] = getOpcodeRnm(73, 527); // subExpressionConstraint
        op[519] = getOpcodeRep((char)1, Character.MAX_VALUE, 520);
        {int[] a = {521,522}; op[520] = getOpcodeCat(a);}
        op[521] = getOpcodeRnm(21, 146); // ws
        op[522] = getOpcodeRnm(72, 523); // dottedExpressionAttribute
        {int[] a = {524,525,526}; op[523] = getOpcodeCat(a);}
        op[524] = getOpcodeRnm(75, 547); // dot
        op[525] = getOpcodeRnm(21, 146); // ws
        op[526] = getOpcodeRnm(105, 718); // eclAttributeName
        {int[] a = {528,532,536}; op[527] = getOpcodeCat(a);}
        op[528] = getOpcodeRep((char)0, (char)1, 529);
        {int[] a = {530,531}; op[529] = getOpcodeCat(a);}
        op[530] = getOpcodeRnm(79, 560); // constraintOperator
        op[531] = getOpcodeRnm(21, 146); // ws
        op[532] = getOpcodeRep((char)0, (char)1, 533);
        {int[] a = {534,535}; op[533] = getOpcodeCat(a);}
        op[534] = getOpcodeRnm(76, 548); // memberOf
        op[535] = getOpcodeRnm(21, 146); // ws
        {int[] a = {537,538}; op[536] = getOpcodeAlt(a);}
        op[537] = getOpcodeRnm(74, 544); // eclFocusConcept
        {int[] a = {539,540,541,542,543}; op[538] = getOpcodeCat(a);}
        {char[] a = {40}; op[539] = getOpcodeTls(a);}
        op[540] = getOpcodeRnm(21, 146); // ws
        op[541] = getOpcodeRnm(65, 477); // expressionConstraint
        op[542] = getOpcodeRnm(21, 146); // ws
        {char[] a = {41}; op[543] = getOpcodeTls(a);}
        {int[] a = {545,546}; op[544] = getOpcodeAlt(a);}
        op[545] = getOpcodeRnm(77, 549); // eclConceptReference
        op[546] = getOpcodeRnm(78, 559); // wildCard
        {char[] a = {46}; op[547] = getOpcodeTls(a);}
        {char[] a = {94}; op[548] = getOpcodeTls(a);}
        {int[] a = {550,551}; op[549] = getOpcodeCat(a);}
        op[550] = getOpcodeRnm(7, 52); // conceptId
        op[551] = getOpcodeRep((char)0, (char)1, 552);
        {int[] a = {553,554,555,556,557,558}; op[552] = getOpcodeCat(a);}
        op[553] = getOpcodeRnm(21, 146); // ws
        {char[] a = {124}; op[554] = getOpcodeTls(a);}
        op[555] = getOpcodeRnm(21, 146); // ws
        op[556] = getOpcodeRnm(8, 53); // term
        op[557] = getOpcodeRnm(21, 146); // ws
        {char[] a = {124}; op[558] = getOpcodeTls(a);}
        {char[] a = {42}; op[559] = getOpcodeTls(a);}
        {int[] a = {561,562,563,564,565,566}; op[560] = getOpcodeAlt(a);}
        op[561] = getOpcodeRnm(82, 569); // childOf
        op[562] = getOpcodeRnm(81, 568); // descendantOrSelfOf
        op[563] = getOpcodeRnm(80, 567); // descendantOf
        op[564] = getOpcodeRnm(85, 572); // parentOf
        op[565] = getOpcodeRnm(84, 571); // ancestorOrSelfOf
        op[566] = getOpcodeRnm(83, 570); // ancestorOf
        {char[] a = {60}; op[567] = getOpcodeTls(a);}
        {char[] a = {60,60}; op[568] = getOpcodeTls(a);}
        {char[] a = {60,33}; op[569] = getOpcodeTls(a);}
        {char[] a = {62}; op[570] = getOpcodeTls(a);}
        {char[] a = {62,62}; op[571] = getOpcodeTls(a);}
        {char[] a = {62,33}; op[572] = getOpcodeTls(a);}
        {int[] a = {574,585}; op[573] = getOpcodeAlt(a);}
        {int[] a = {575,578,581,584}; op[574] = getOpcodeCat(a);}
        {int[] a = {576,577}; op[575] = getOpcodeAlt(a);}
        {char[] a = {97}; op[576] = getOpcodeTls(a);}
        {char[] a = {65}; op[577] = getOpcodeTls(a);}
        {int[] a = {579,580}; op[578] = getOpcodeAlt(a);}
        {char[] a = {110}; op[579] = getOpcodeTls(a);}
        {char[] a = {78}; op[580] = getOpcodeTls(a);}
        {int[] a = {582,583}; op[581] = getOpcodeAlt(a);}
        {char[] a = {100}; op[582] = getOpcodeTls(a);}
        {char[] a = {68}; op[583] = getOpcodeTls(a);}
        op[584] = getOpcodeRnm(110, 738); // mws
        {char[] a = {44}; op[585] = getOpcodeTls(a);}
        {int[] a = {587,590,593}; op[586] = getOpcodeCat(a);}
        {int[] a = {588,589}; op[587] = getOpcodeAlt(a);}
        {char[] a = {111}; op[588] = getOpcodeTls(a);}
        {char[] a = {79}; op[589] = getOpcodeTls(a);}
        {int[] a = {591,592}; op[590] = getOpcodeAlt(a);}
        {char[] a = {114}; op[591] = getOpcodeTls(a);}
        {char[] a = {82}; op[592] = getOpcodeTls(a);}
        op[593] = getOpcodeRnm(110, 738); // mws
        {int[] a = {595,598,601,604,607,610}; op[594] = getOpcodeCat(a);}
        {int[] a = {596,597}; op[595] = getOpcodeAlt(a);}
        {char[] a = {109}; op[596] = getOpcodeTls(a);}
        {char[] a = {77}; op[597] = getOpcodeTls(a);}
        {int[] a = {599,600}; op[598] = getOpcodeAlt(a);}
        {char[] a = {105}; op[599] = getOpcodeTls(a);}
        {char[] a = {73}; op[600] = getOpcodeTls(a);}
        {int[] a = {602,603}; op[601] = getOpcodeAlt(a);}
        {char[] a = {110}; op[602] = getOpcodeTls(a);}
        {char[] a = {78}; op[603] = getOpcodeTls(a);}
        {int[] a = {605,606}; op[604] = getOpcodeAlt(a);}
        {char[] a = {117}; op[605] = getOpcodeTls(a);}
        {char[] a = {85}; op[606] = getOpcodeTls(a);}
        {int[] a = {608,609}; op[607] = getOpcodeAlt(a);}
        {char[] a = {115}; op[608] = getOpcodeTls(a);}
        {char[] a = {83}; op[609] = getOpcodeTls(a);}
        op[610] = getOpcodeRnm(110, 738); // mws
        {int[] a = {612,613,614}; op[611] = getOpcodeCat(a);}
        op[612] = getOpcodeRnm(92, 630); // subRefinement
        op[613] = getOpcodeRnm(21, 146); // ws
        op[614] = getOpcodeRep((char)0, (char)1, 615);
        {int[] a = {616,617}; op[615] = getOpcodeAlt(a);}
        op[616] = getOpcodeRnm(90, 618); // conjunctionRefinementSet
        op[617] = getOpcodeRnm(91, 624); // disjunctionRefinementSet
        op[618] = getOpcodeRep((char)1, Character.MAX_VALUE, 619);
        {int[] a = {620,621,622,623}; op[619] = getOpcodeCat(a);}
        op[620] = getOpcodeRnm(21, 146); // ws
        op[621] = getOpcodeRnm(86, 573); // conjunction
        op[622] = getOpcodeRnm(21, 146); // ws
        op[623] = getOpcodeRnm(92, 630); // subRefinement
        op[624] = getOpcodeRep((char)1, Character.MAX_VALUE, 625);
        {int[] a = {626,627,628,629}; op[625] = getOpcodeCat(a);}
        op[626] = getOpcodeRnm(21, 146); // ws
        op[627] = getOpcodeRnm(87, 586); // disjunction
        op[628] = getOpcodeRnm(21, 146); // ws
        op[629] = getOpcodeRnm(92, 630); // subRefinement
        {int[] a = {631,632,633}; op[630] = getOpcodeAlt(a);}
        op[631] = getOpcodeRnm(93, 639); // eclAttributeSet
        op[632] = getOpcodeRnm(97, 666); // eclAttributeGroup
        {int[] a = {634,635,636,637,638}; op[633] = getOpcodeCat(a);}
        {char[] a = {40}; op[634] = getOpcodeTls(a);}
        op[635] = getOpcodeRnm(21, 146); // ws
        op[636] = getOpcodeRnm(89, 611); // eclRefinement
        op[637] = getOpcodeRnm(21, 146); // ws
        {char[] a = {41}; op[638] = getOpcodeTls(a);}
        {int[] a = {640,641,642}; op[639] = getOpcodeCat(a);}
        op[640] = getOpcodeRnm(96, 658); // subAttributeSet
        op[641] = getOpcodeRnm(21, 146); // ws
        op[642] = getOpcodeRep((char)0, (char)1, 643);
        {int[] a = {644,645}; op[643] = getOpcodeAlt(a);}
        op[644] = getOpcodeRnm(94, 646); // conjunctionAttributeSet
        op[645] = getOpcodeRnm(95, 652); // disjunctionAttributeSet
        op[646] = getOpcodeRep((char)1, Character.MAX_VALUE, 647);
        {int[] a = {648,649,650,651}; op[647] = getOpcodeCat(a);}
        op[648] = getOpcodeRnm(21, 146); // ws
        op[649] = getOpcodeRnm(86, 573); // conjunction
        op[650] = getOpcodeRnm(21, 146); // ws
        op[651] = getOpcodeRnm(96, 658); // subAttributeSet
        op[652] = getOpcodeRep((char)1, Character.MAX_VALUE, 653);
        {int[] a = {654,655,656,657}; op[653] = getOpcodeCat(a);}
        op[654] = getOpcodeRnm(21, 146); // ws
        op[655] = getOpcodeRnm(87, 586); // disjunction
        op[656] = getOpcodeRnm(21, 146); // ws
        op[657] = getOpcodeRnm(96, 658); // subAttributeSet
        {int[] a = {659,660}; op[658] = getOpcodeAlt(a);}
        op[659] = getOpcodeRnm(98, 678); // eclAttribute
        {int[] a = {661,662,663,664,665}; op[660] = getOpcodeCat(a);}
        {char[] a = {40}; op[661] = getOpcodeTls(a);}
        op[662] = getOpcodeRnm(21, 146); // ws
        op[663] = getOpcodeRnm(93, 639); // eclAttributeSet
        op[664] = getOpcodeRnm(21, 146); // ws
        {char[] a = {41}; op[665] = getOpcodeTls(a);}
        {int[] a = {667,673,674,675,676,677}; op[666] = getOpcodeCat(a);}
        op[667] = getOpcodeRep((char)0, (char)1, 668);
        {int[] a = {669,670,671,672}; op[668] = getOpcodeCat(a);}
        {char[] a = {91}; op[669] = getOpcodeTls(a);}
        op[670] = getOpcodeRnm(99, 707); // cardinality
        {char[] a = {93}; op[671] = getOpcodeTls(a);}
        op[672] = getOpcodeRnm(21, 146); // ws
        {char[] a = {123}; op[673] = getOpcodeTls(a);}
        op[674] = getOpcodeRnm(21, 146); // ws
        op[675] = getOpcodeRnm(93, 639); // eclAttributeSet
        op[676] = getOpcodeRnm(21, 146); // ws
        {char[] a = {125}; op[677] = getOpcodeTls(a);}
        {int[] a = {679,685,689,690,691}; op[678] = getOpcodeCat(a);}
        op[679] = getOpcodeRep((char)0, (char)1, 680);
        {int[] a = {681,682,683,684}; op[680] = getOpcodeCat(a);}
        {char[] a = {91}; op[681] = getOpcodeTls(a);}
        op[682] = getOpcodeRnm(99, 707); // cardinality
        {char[] a = {93}; op[683] = getOpcodeTls(a);}
        op[684] = getOpcodeRnm(21, 146); // ws
        op[685] = getOpcodeRep((char)0, (char)1, 686);
        {int[] a = {687,688}; op[686] = getOpcodeCat(a);}
        op[687] = getOpcodeRnm(104, 717); // reverseFlag
        op[688] = getOpcodeRnm(21, 146); // ws
        op[689] = getOpcodeRnm(105, 718); // eclAttributeName
        op[690] = getOpcodeRnm(21, 146); // ws
        {int[] a = {692,696,701}; op[691] = getOpcodeAlt(a);}
        {int[] a = {693,694,695}; op[692] = getOpcodeCat(a);}
        op[693] = getOpcodeRnm(106, 719); // expressionComparisonOperator
        op[694] = getOpcodeRnm(21, 146); // ws
        op[695] = getOpcodeRnm(73, 527); // subExpressionConstraint
        {int[] a = {697,698,699,700}; op[696] = getOpcodeCat(a);}
        op[697] = getOpcodeRnm(107, 722); // numericComparisonOperator
        op[698] = getOpcodeRnm(21, 146); // ws
        {char[] a = {35}; op[699] = getOpcodeTls(a);}
        op[700] = getOpcodeRnm(17, 123); // numericValue
        {int[] a = {702,703,704,705,706}; op[701] = getOpcodeCat(a);}
        op[702] = getOpcodeRnm(108, 729); // stringComparisonOperator
        op[703] = getOpcodeRnm(21, 146); // ws
        op[704] = getOpcodeRnm(26, 156); // QM
        op[705] = getOpcodeRnm(16, 119); // stringValue
        op[706] = getOpcodeRnm(26, 156); // QM
        {int[] a = {708,709,710}; op[707] = getOpcodeCat(a);}
        op[708] = getOpcodeRnm(100, 711); // minValue
        op[709] = getOpcodeRnm(101, 712); // to
        op[710] = getOpcodeRnm(102, 713); // maxValue
        op[711] = getOpcodeRnm(109, 732); // nonNegativeIntegerValue
        {char[] a = {46,46}; op[712] = getOpcodeTls(a);}
        {int[] a = {714,715}; op[713] = getOpcodeAlt(a);}
        op[714] = getOpcodeRnm(109, 732); // nonNegativeIntegerValue
        op[715] = getOpcodeRnm(103, 716); // many
        {char[] a = {42}; op[716] = getOpcodeTls(a);}
        {char[] a = {82}; op[717] = getOpcodeTls(a);}
        op[718] = getOpcodeRnm(73, 527); // subExpressionConstraint
        {int[] a = {720,721}; op[719] = getOpcodeAlt(a);}
        {char[] a = {61}; op[720] = getOpcodeTls(a);}
        {char[] a = {33,61}; op[721] = getOpcodeTls(a);}
        {int[] a = {723,724,725,726,727,728}; op[722] = getOpcodeAlt(a);}
        {char[] a = {61}; op[723] = getOpcodeTls(a);}
        {char[] a = {33,61}; op[724] = getOpcodeTls(a);}
        {char[] a = {60,61}; op[725] = getOpcodeTls(a);}
        {char[] a = {60}; op[726] = getOpcodeTls(a);}
        {char[] a = {62,61}; op[727] = getOpcodeTls(a);}
        {char[] a = {62}; op[728] = getOpcodeTls(a);}
        {int[] a = {730,731}; op[729] = getOpcodeAlt(a);}
        {char[] a = {61}; op[730] = getOpcodeTls(a);}
        {char[] a = {33,61}; op[731] = getOpcodeTls(a);}
        {int[] a = {733,737}; op[732] = getOpcodeAlt(a);}
        {int[] a = {734,735}; op[733] = getOpcodeCat(a);}
        op[734] = getOpcodeRnm(30, 160); // digitNonZero
        op[735] = getOpcodeRep((char)0, Character.MAX_VALUE, 736);
        op[736] = getOpcodeRnm(28, 158); // digit
        op[737] = getOpcodeRnm(29, 159); // zero
        op[738] = getOpcodeRep((char)1, Character.MAX_VALUE, 739);
        {int[] a = {740,741,742,743,744}; op[739] = getOpcodeAlt(a);}
        op[740] = getOpcodeRnm(22, 152); // SP
        op[741] = getOpcodeRnm(23, 153); // HTAB
        op[742] = getOpcodeRnm(24, 154); // CR
        op[743] = getOpcodeRnm(25, 155); // LF
        op[744] = getOpcodeRnm(111, 745); // comment
        {int[] a = {746,747,751}; op[745] = getOpcodeCat(a);}
        {char[] a = {47,42}; op[746] = getOpcodeTls(a);}
        op[747] = getOpcodeRep((char)0, Character.MAX_VALUE, 748);
        {int[] a = {749,750}; op[748] = getOpcodeAlt(a);}
        op[749] = getOpcodeRnm(112, 752); // nonStarChar
        op[750] = getOpcodeRnm(113, 762); // starWithNonFSlash
        {char[] a = {42,47}; op[751] = getOpcodeTls(a);}
        {int[] a = {753,754,755,756,757,758,759,760,761}; op[752] = getOpcodeAlt(a);}
        op[753] = getOpcodeRnm(22, 152); // SP
        op[754] = getOpcodeRnm(23, 153); // HTAB
        op[755] = getOpcodeRnm(24, 154); // CR
        op[756] = getOpcodeRnm(25, 155); // LF
        op[757] = getOpcodeTrg((char)33, (char)41);
        op[758] = getOpcodeTrg((char)43, (char)126);
        op[759] = getOpcodeRnm(34, 184); // UTF8-2
        op[760] = getOpcodeRnm(35, 187); // UTF8-3
        op[761] = getOpcodeRnm(36, 204); // UTF8-4
        {int[] a = {763,764}; op[762] = getOpcodeCat(a);}
        {char[] a = {42}; op[763] = getOpcodeTbs(a);}
        op[764] = getOpcodeRnm(114, 765); // nonFSlash
        {int[] a = {766,767,768,769,770,771,772,773,774}; op[765] = getOpcodeAlt(a);}
        op[766] = getOpcodeRnm(22, 152); // SP
        op[767] = getOpcodeRnm(23, 153); // HTAB
        op[768] = getOpcodeRnm(24, 154); // CR
        op[769] = getOpcodeRnm(25, 155); // LF
        op[770] = getOpcodeTrg((char)33, (char)46);
        op[771] = getOpcodeTrg((char)48, (char)126);
        op[772] = getOpcodeRnm(34, 184); // UTF8-2
        op[773] = getOpcodeRnm(35, 187); // UTF8-3
        op[774] = getOpcodeRnm(36, 204); // UTF8-4
        return op;
    }

    public static void display(PrintStream out){
        out.println(";");
        out.println("; org.ihtsdo.rvf.util.ExpressionTemplateGrammar");
        out.println(";");
        out.println("; Compositional Grammar v2.3.1 with slot references (in blue)");
        out.println("expressionTemplate =  ws [ (definitionStatus / tokenReplacementSlot) ws] subExpression ws");
        out.println("subExpression = focusConcept [ws \":\" ws refinement]");
        out.println("definitionStatus = equivalentTo / subtypeOf");
        out.println("equivalentTo = \"===\" ");
        out.println("subtypeOf = \"<<<\"");
        out.println("focusConcept = [templateInformationSlot ws] conceptReference *(ws \"+\" ws [templateInformationSlot ws] conceptReference) ");
        out.println("conceptReference = conceptReplacementSlot / expressionReplacementSlot / ( conceptId [ws \"|\" ws  term ws \"|\"] )");
        out.println("conceptId = sctId");
        out.println("term = nonwsNonPipe *( *SP nonwsNonPipe )");
        out.println("refinement = (attributeSet / attributeGroup) *( ws [\",\" ws] attributeGroup )");
        out.println("attributeGroup =   [ templateInformationSlot ws ] \"{\" ws attributeSet ws \"}\"");
        out.println("attributeSet = attribute *(ws \",\" ws attribute)");
        out.println("attribute = [ templateInformationSlot ws ] attributeName ws \"=\" ws attributeValue");
        out.println("attributeName = conceptReference");
        out.println("attributeValue = expressionValue / QM stringValue QM / \"#\" numericValue / concreteValueReplacementSlot");
        out.println("expressionValue = conceptReference / \"(\" ws subExpression ws \")\"");
        out.println("stringValue = 1*(anyNonEscapedChar / escapedChar)");
        out.println("numericValue = [\"-\"/\"+\"] (decimalValue / integerValue)");
        out.println("integerValue =  digitNonZero *digit / zero");
        out.println("decimalValue = integerValue \".\" 1*digit");
        out.println("sctId = digitNonZero 5*17( digit )");
        out.println("ws = *( SP / HTAB / CR / LF ) ; optional white space");
        out.println("SP = %x20 ; space");
        out.println("HTAB = %x09 ; tab");
        out.println("CR = %x0D ; carriage return");
        out.println("LF = %x0A ; line feed");
        out.println("QM = %x22 ; quotation mark");
        out.println("BS = %x5C ; back slash");
        out.println("digit = %x30-39");
        out.println("zero = %x30");
        out.println("digitNonZero = %x31-39");
        out.println("nonwsNonPipe = %x21-7B / %x7D-7E / UTF8-2 / UTF8-3 / UTF8-4");
        out.println("anyNonEscapedChar = HTAB / CR / LF / %x20-21 / %x23-5B / %x5D-7E / UTF8-2 / UTF8-3 / UTF8-4");
        out.println("escapedChar = BS QM / BS BS");
        out.println("UTF8-2 = %xC2-DF UTF8-tail");
        out.println("UTF8-3 = %xE0 %xA0-BF UTF8-tail / %xE1-EC 2( UTF8-tail ) / %xED %x80-9F UTF8-tail / %xEE-EF 2( UTF8-tail )");
        out.println("UTF8-4 = %xF0 %x90-BF 2( UTF8-tail ) / %xF1-F3 3( UTF8-tail ) / %xF4 %x80-8F 2( UTF8-tail )");
        out.println("UTF8-tail = %x80-BF");
        out.println("; Template Syntax v1");
        out.println("templateSlot =  templateReplacementSlot / templateInformationSlot");
        out.println("templateReplacementSlot = conceptReplacementSlot / expressionReplacementSlot / tokenReplacementSlot / concreteValueReplacementSlot");
        out.println("conceptReplacementSlot = \"[[\" ws \"+\" ws \"id\" ws [ \"(\" ws expressionConstraint ws \")\" ws] [slotName ws] \"]]\"");
        out.println("expressionReplacementSlot = \"[[\" ws \"+\" ws [\"scg\" ws] [ \"(\" ws expressionConstraint ws \")\" ws] [slotName ws] \"]]\"");
        out.println("tokenReplacementSlot = \"[[\" ws \"+\" ws \"tok\" ws [ \"(\" ws slotTokenSet ws \")\" ws] [slotName ws] \"]]\"");
        out.println("concreteValueReplacementSlot = stringReplacementSlot / integerReplacementSlot / decimalReplacementSlot");
        out.println("stringReplacementSlot = \"[[\" ws \"+\" ws \"str\" ws [ \"(\" ws slotStringSet ws \")\" ws] [slotName ws] \"]]\"");
        out.println("integerReplacementSlot = \"[[\" ws \"+\" ws \"int\" ws [ \"(\" ws slotIntegerSet ws \")\" ws] [slotName ws] \"]]\"");
        out.println("decimalReplacementSlot = \"[[\" ws \"+\" ws \"dec\" ws [ \"(\" ws slotDecimalSet ws \")\" ws] [slotName ws] \"]]\" ");
        out.println("slotTokenSet = slotToken *(mws slotToken)");
        out.println("slotStringSet = slotString *(mws slotString)");
        out.println("slotIntegerSet = ( \"#\" integerValue / slotIntegerRange) *(mws (\"#\" integerValue / slotIntegerRange))");
        out.println("slotDecimalSet = ( \"#\" decimalValue / slotDecimalRange) *(mws (\"#\" decimalValue / slotDecimalRange))");
        out.println("slotIntegerRange = ( slotIntegerMinimum to [ slotIntegerMaximum ] ) / ( to slotIntegerMaximum )");
        out.println("slotIntegerMinimum = [ exclusiveMinimum ] \"#\" integerValue");
        out.println("slotIntegerMaximum = [ exclusiveMaximum ] \"#\" integerValue");
        out.println("slotDecimalRange = ( slotDecimalMinimum to [ slotDecimalMaximum ] ) / ( to slotDecimalMaximum )");
        out.println("slotDecimalMinimum = [ exclusiveMinimum ] \"#\" DecimalValue");
        out.println("slotDecimalMaximum = [ exclusiveMaximum ] \"#\" DecimalValue");
        out.println("exclusiveMinimum = \">\"");
        out.println("exclusiveMaximum = \"<\"");
        out.println("slotName = \"@\" (nonQuoteStringValue / slotString)");
        out.println("slotToken = definitionStatus / memberOf / constraintOperator / conjunction / disjunction / exclusion / reverseFlag / expressionComparisonOperator / numericComparisonOperator / stringComparisonOperator");
        out.println("slotString = QM stringValue QM");
        out.println("nonQuoteStringValue = *(%x21 / %x23-26 / %x28-3F / %x41-5A / %x5C / %x5E-7E)   ; string with no ws, quotes, at or square brackets");
        out.println("templateInformationSlot = \"[[\" ws slotInformation ws \"]]\"");
        out.println("slotInformation = [cardinality ws] [slotName ws]");
        out.println("; Expression Constraint Language v1.3 - Note that some rules are commented out because they are repeated in the Compositional Grammar rules above.");
        out.println("expressionConstraint = ws ( refinedExpressionConstraint / compoundExpressionConstraint / dottedExpressionConstraint / subExpressionConstraint ) ws");
        out.println("refinedExpressionConstraint = subExpressionConstraint ws \":\" ws eclRefinement");
        out.println("compoundExpressionConstraint = conjunctionExpressionConstraint / disjunctionExpressionConstraint / exclusionExpressionConstraint");
        out.println("conjunctionExpressionConstraint = subExpressionConstraint 1*(ws conjunction ws subExpressionConstraint)");
        out.println("disjunctionExpressionConstraint = subExpressionConstraint 1*(ws disjunction ws subExpressionConstraint)");
        out.println("exclusionExpressionConstraint = subExpressionConstraint ws exclusion ws subExpressionConstraint");
        out.println("dottedExpressionConstraint = subExpressionConstraint 1*(ws dottedExpressionAttribute)");
        out.println("dottedExpressionAttribute = dot ws eclAttributeName");
        out.println("subExpressionConstraint = [constraintOperator ws] [memberOf ws] (eclFocusConcept / \"(\" ws expressionConstraint ws \")\")");
        out.println("eclFocusConcept = eclConceptReference / wildCard");
        out.println("dot = \".\"");
        out.println("memberOf = \"^\"");
        out.println("eclConceptReference = conceptId [ws \"|\" ws term ws \"|\"]");
        out.println("; conceptId = sctId");
        out.println("; term = 1*nonwsNonPipe *( 1*SP 1*nonwsNonPipe )");
        out.println("wildCard = \"*\"");
        out.println("constraintOperator = childOf / descendantOrSelfOf / descendantOf / parentOf / ancestorOrSelfOf / ancestorOf");
        out.println("descendantOf = \"<\"");
        out.println("descendantOrSelfOf = \"<<\"");
        out.println("childOf = \"<!\"");
        out.println("ancestorOf = \">\"");
        out.println("ancestorOrSelfOf = \">>\"");
        out.println("parentOf = \">!\"");
        out.println("conjunction = ((\"a\"/\"A\") (\"n\"/\"N\") (\"d\"/\"D\") mws) / \",\"");
        out.println("disjunction = (\"o\"/\"O\") (\"r\"/\"R\") mws");
        out.println("exclusion = (\"m\"/\"M\") (\"i\"/\"I\") (\"n\"/\"N\") (\"u\"/\"U\") (\"s\"/\"S\") mws");
        out.println("eclRefinement = subRefinement ws [conjunctionRefinementSet / disjunctionRefinementSet]");
        out.println("conjunctionRefinementSet = 1*(ws conjunction ws subRefinement)");
        out.println("disjunctionRefinementSet = 1*(ws disjunction ws subRefinement)");
        out.println("subRefinement = eclAttributeSet / eclAttributeGroup / \"(\" ws eclRefinement ws \")\"");
        out.println("eclAttributeSet = subAttributeSet ws [conjunctionAttributeSet / disjunctionAttributeSet]");
        out.println("conjunctionAttributeSet = 1*(ws conjunction ws subAttributeSet)");
        out.println("disjunctionAttributeSet = 1*(ws disjunction ws subAttributeSet)");
        out.println("subAttributeSet = eclAttribute / \"(\" ws eclAttributeSet ws \")\"");
        out.println("eclAttributeGroup = [\"[\" cardinality \"]\" ws] \"{\" ws eclAttributeSet ws \"}\"");
        out.println("eclAttribute = [\"[\" cardinality \"]\" ws] [reverseFlag ws] eclAttributeName ws (expressionComparisonOperator ws subExpressionConstraint / numericComparisonOperator ws \"#\" numericValue / stringComparisonOperator ws QM stringValue QM)");
        out.println("cardinality = minValue to maxValue");
        out.println("minValue = nonNegativeIntegerValue");
        out.println("to = \"..\"");
        out.println("maxValue = nonNegativeIntegerValue / many");
        out.println("many = \"*\"");
        out.println("reverseFlag = \"R\"");
        out.println("eclAttributeName = subExpressionConstraint");
        out.println("expressionComparisonOperator = \"=\" / \"!=\"");
        out.println("numericComparisonOperator = \"=\" / \"!=\" / \"<=\" / \"<\" / \">=\" / \">\"");
        out.println("stringComparisonOperator = \"=\" / \"!=\"");
        out.println("; numericValue = [\"-\"/\"+\"] (decimalValue / integerValue)");
        out.println("; stringValue = 1*(anyNonEscapedChar / escapedChar)");
        out.println("; integerValue =  digitNonZero *digit / zero");
        out.println("; decimalValue = integerValue \".\" 1*digit");
        out.println("nonNegativeIntegerValue = (digitNonZero *digit ) / zero");
        out.println("; sctId = digitNonZero 5*17( digit )");
        out.println("; ws = *( SP / HTAB / CR / LF / comment ) ; optional white space");
        out.println("mws = 1*( SP / HTAB / CR / LF / comment ) ; mandatory white space");
        out.println("comment = \"/*\" *(nonStarChar / starWithNonFSlash) \"*/\"");
        out.println("nonStarChar = SP / HTAB / CR / LF / %x21-29 / %x2B-7E /UTF8-2 / UTF8-3 / UTF8-4");
        out.println("starWithNonFSlash = %x2A nonFSlash");
        out.println("nonFSlash = SP / HTAB / CR / LF / %x21-2E / %x30-7E /UTF8-2 / UTF8-3 / UTF8-4");
        out.println("; SP = %x20 ; space");
        out.println("; HTAB = %x09 ; tab");
        out.println("; CR = %x0D ; carriage return");
        out.println("; LF = %x0A ; line feed");
        out.println("; QM = %x22 ; quotation mark");
        out.println("; BS = %x5C ; back slash");
        out.println("; digit = %x30-39");
        out.println("; zero = %x30");
        out.println("; digitNonZero = %x31-39");
        out.println("; nonwsNonPipe = %x21-7B / %x7D-7E / UTF8-2 / UTF8-3 / UTF8-4");
        out.println("; anyNonEscapedChar = SP / HTAB / CR / LF / %x20-21 / %x23-5B / %x5D-7E / UTF8-2 / UTF8-3 / UTF8-4");
        out.println("; escapedChar = BS QM / BS BS");
        out.println("; UTF8-2 = %xC2-DF UTF8-tail");
        out.println("; UTF8-3 = %xE0 %xA0-BF UTF8-tail / %xE1-EC 2( UTF8-tail ) / %xED %x80-9F UTF8-tail / %xEE-EF 2( UTF8-tail )");
        out.println("; UTF8-4 = %xF0 %x90-BF 2( UTF8-tail ) / %xF1-F3 3( UTF8-tail ) / %xF4 %x80-8F 2( UTF8-tail )");
        out.println("; UTF8-tail = %x80-BF");
    }
}
