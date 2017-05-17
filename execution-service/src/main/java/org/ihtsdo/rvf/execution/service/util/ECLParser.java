package org.ihtsdo.rvf.execution.service.util;

import apg.Grammar;

import java.io.PrintStream;

/**
 * User: huyle
 * Date: 5/12/2017
 * Time: 3:02 PM
 */
public class ECLParser extends Grammar {
    // public API
    public static Grammar getInstance(){
        if(factoryInstance == null){
            factoryInstance = new ECLParser(getRules(), getUdts(), getOpcodes());
        }
        return factoryInstance;
    }

    // rule name enum
    public static int ruleCount = 75;
    public enum RuleNames{
        ANCESTOROF("ancestorOf", 20, 115, 1),
        ANCESTORORSELFOF("ancestorOrSelfOf", 21, 116, 1),
        ANYNONESCAPEDCHAR("anyNonEscapedChar", 69, 378, 11),
        ATTRIBUTEOPERATOR("attributeOperator", 42, 267, 3),
        BS("BS", 64, 368, 1),
        CARDINALITY("cardinality", 36, 256, 4),
        CHILDOF("childOf", 19, 114, 1),
        COMMENT("comment", 55, 333, 7),
        COMPOUNDEXPRESSIONCONSTRAINT("compoundExpressionConstraint", 2, 14, 4),
        CONCEPTID("conceptId", 13, 94, 1),
        CONCEPTREFERENCE("conceptReference", 12, 84, 10),
        CONJUNCTION("conjunction", 23, 118, 13),
        CONJUNCTIONATTRIBUTESET("conjunctionAttributeSet", 31, 191, 6),
        CONJUNCTIONEXPRESSIONCONSTRAINT("conjunctionExpressionConstraint", 3, 18, 8),
        CONJUNCTIONREFINEMENTSET("conjunctionRefinementSet", 27, 163, 6),
        CONSTRAINTOPERATOR("constraintOperator", 16, 105, 7),
        CR("CR", 61, 365, 1),
        DECIMALVALUE("decimalValue", 50, 304, 5),
        DESCENDANTOF("descendantOf", 17, 112, 1),
        DESCENDANTORSELFOF("descendantOrSelfOf", 18, 113, 1),
        DIGIT("digit", 65, 369, 1),
        DIGITNONZERO("digitNonZero", 67, 371, 1),
        DISJUNCTION("disjunction", 24, 131, 8),
        DISJUNCTIONATTRIBUTESET("disjunctionAttributeSet", 32, 197, 6),
        DISJUNCTIONEXPRESSIONCONSTRAINT("disjunctionExpressionConstraint", 4, 26, 8),
        DISJUNCTIONREFINEMENTSET("disjunctionRefinementSet", 28, 169, 6),
        DOT("dot", 10, 82, 1),
        DOTTEDEXPRESSIONCONSTRAINT("dottedExpressionConstraint", 6, 40, 12),
        ECLATTRIBUTE("eclAttribute", 35, 223, 33),
        ECLATTRIBUTEGROUP("eclAttributeGroup", 34, 211, 12),
        ECLATTRIBUTENAME("eclAttributeName", 43, 270, 3),
        ECLATTRIBUTESET("eclAttributeSet", 30, 184, 7),
        ECLFOCUSCONCEPT("eclFocusConcept", 9, 79, 3),
        ECLREFINEMENT("eclRefinement", 26, 156, 7),
        ESCAPEDCHAR("escapedChar", 70, 389, 7),
        EXCLUSION("exclusion", 25, 139, 17),
        EXCLUSIONEXPRESSIONCONSTRAINT("exclusionExpressionConstraint", 5, 34, 6),
        EXPRESSIONCOMPARISONOPERATOR("expressionComparisonOperator", 44, 273, 3),
        EXPRESSIONCONSTRAINT("expressionConstraint", 0, 0, 8),
        HTAB("HTAB", 60, 364, 1),
        INTEGERVALUE("integerValue", 49, 298, 6),
        LF("LF", 62, 366, 1),
        MANY("many", 40, 265, 1),
        MAXVALUE("maxValue", 39, 262, 3),
        MEMBEROF("memberOf", 11, 83, 1),
        MINVALUE("minValue", 37, 260, 1),
        MWS("mws", 54, 326, 7),
        NONFSLASH("nonFSlash", 58, 353, 10),
        NONNEGATIVEINTEGERVALUE("nonNegativeIntegerValue", 51, 309, 6),
        NONSTARCHAR("nonStarChar", 56, 340, 10),
        NONWSNONPIPE("nonwsNonPipe", 68, 372, 6),
        NUMERICCOMPARISONOPERATOR("numericComparisonOperator", 45, 276, 7),
        NUMERICVALUE("numericValue", 47, 286, 8),
        PARENTOF("parentOf", 22, 117, 1),
        QM("QM", 63, 367, 1),
        REFINEDEXPRESSIONCONSTRAINT("refinedExpressionConstraint", 1, 8, 6),
        REVERSEFLAG("reverseFlag", 41, 266, 1),
        SCTID("sctId", 52, 315, 4),
        SIMPLEEXPRESSIONCONSTRAINT("simpleExpressionConstraint", 8, 69, 10),
        SP("SP", 59, 363, 1),
        STARWITHNONFSLASH("starWithNonFSlash", 57, 350, 3),
        STRINGCOMPARISONOPERATOR("stringComparisonOperator", 46, 283, 3),
        STRINGVALUE("stringValue", 48, 294, 4),
        SUBATTRIBUTESET("subAttributeSet", 33, 203, 8),
        SUBEXPRESSIONCONSTRAINT("subExpressionConstraint", 7, 52, 17),
        SUBREFINEMENT("subRefinement", 29, 175, 9),
        TERM("term", 14, 95, 9),
        TO("to", 38, 261, 1),
        UTF8_2("UTF8-2", 71, 396, 3),
        UTF8_3("UTF8-3", 72, 399, 17),
        UTF8_4("UTF8-4", 73, 416, 15),
        UTF8_TAIL("UTF8-tail", 74, 431, 1),
        WILDCARD("wildCard", 15, 104, 1),
        WS("ws", 53, 319, 7),
        ZERO("zero", 66, 370, 1);
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
    private static ECLParser factoryInstance = null;
    private ECLParser(Rule[] rules, Udt[] udts, Opcode[] opcodes){
        super(rules, udts, opcodes);
    }

    private static Rule[] getRules(){
        Rule[] rules = new Rule[75];
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
        Opcode[] op = new Opcode[432];
        {int[] a = {1,2,7}; op[0] = getOpcodeCat(a);}
        op[1] = getOpcodeRnm(53, 319); // ws
        {int[] a = {3,4,5,6}; op[2] = getOpcodeAlt(a);}
        op[3] = getOpcodeRnm(1, 8); // refinedExpressionConstraint
        op[4] = getOpcodeRnm(2, 14); // compoundExpressionConstraint
        op[5] = getOpcodeRnm(6, 40); // dottedExpressionConstraint
        op[6] = getOpcodeRnm(7, 52); // subExpressionConstraint
        op[7] = getOpcodeRnm(53, 319); // ws
        {int[] a = {9,10,11,12,13}; op[8] = getOpcodeCat(a);}
        op[9] = getOpcodeRnm(8, 69); // simpleExpressionConstraint
        op[10] = getOpcodeRnm(53, 319); // ws
        {char[] a = {58}; op[11] = getOpcodeTls(a);}
        op[12] = getOpcodeRnm(53, 319); // ws
        op[13] = getOpcodeRnm(26, 156); // eclRefinement
        {int[] a = {15,16,17}; op[14] = getOpcodeAlt(a);}
        op[15] = getOpcodeRnm(3, 18); // conjunctionExpressionConstraint
        op[16] = getOpcodeRnm(4, 26); // disjunctionExpressionConstraint
        op[17] = getOpcodeRnm(5, 34); // exclusionExpressionConstraint
        {int[] a = {19,20}; op[18] = getOpcodeCat(a);}
        op[19] = getOpcodeRnm(7, 52); // subExpressionConstraint
        op[20] = getOpcodeRep((char)1, Character.MAX_VALUE, 21);
        {int[] a = {22,23,24,25}; op[21] = getOpcodeCat(a);}
        op[22] = getOpcodeRnm(53, 319); // ws
        op[23] = getOpcodeRnm(23, 118); // conjunction
        op[24] = getOpcodeRnm(53, 319); // ws
        op[25] = getOpcodeRnm(7, 52); // subExpressionConstraint
        {int[] a = {27,28}; op[26] = getOpcodeCat(a);}
        op[27] = getOpcodeRnm(7, 52); // subExpressionConstraint
        op[28] = getOpcodeRep((char)1, Character.MAX_VALUE, 29);
        {int[] a = {30,31,32,33}; op[29] = getOpcodeCat(a);}
        op[30] = getOpcodeRnm(53, 319); // ws
        op[31] = getOpcodeRnm(24, 131); // disjunction
        op[32] = getOpcodeRnm(53, 319); // ws
        op[33] = getOpcodeRnm(7, 52); // subExpressionConstraint
        {int[] a = {35,36,37,38,39}; op[34] = getOpcodeCat(a);}
        op[35] = getOpcodeRnm(7, 52); // subExpressionConstraint
        op[36] = getOpcodeRnm(53, 319); // ws
        op[37] = getOpcodeRnm(25, 139); // exclusion
        op[38] = getOpcodeRnm(53, 319); // ws
        op[39] = getOpcodeRnm(7, 52); // subExpressionConstraint
        {int[] a = {41,42}; op[40] = getOpcodeCat(a);}
        op[41] = getOpcodeRnm(7, 52); // subExpressionConstraint
        op[42] = getOpcodeRep((char)1, Character.MAX_VALUE, 43);
        {int[] a = {44,45,46,47,51}; op[43] = getOpcodeCat(a);}
        op[44] = getOpcodeRnm(53, 319); // ws
        op[45] = getOpcodeRnm(10, 82); // dot
        op[46] = getOpcodeRnm(53, 319); // ws
        op[47] = getOpcodeRep((char)0, (char)1, 48);
        {int[] a = {49,50}; op[48] = getOpcodeCat(a);}
        op[49] = getOpcodeRnm(42, 267); // attributeOperator
        op[50] = getOpcodeRnm(53, 319); // ws
        op[51] = getOpcodeRnm(43, 270); // eclAttributeName
        {int[] a = {53,57,61}; op[52] = getOpcodeCat(a);}
        op[53] = getOpcodeRep((char)0, (char)1, 54);
        {int[] a = {55,56}; op[54] = getOpcodeCat(a);}
        op[55] = getOpcodeRnm(16, 105); // constraintOperator
        op[56] = getOpcodeRnm(53, 319); // ws
        op[57] = getOpcodeRep((char)0, (char)1, 58);
        {int[] a = {59,60}; op[58] = getOpcodeCat(a);}
        op[59] = getOpcodeRnm(11, 83); // memberOf
        op[60] = getOpcodeRnm(53, 319); // ws
        {int[] a = {62,63}; op[61] = getOpcodeAlt(a);}
        op[62] = getOpcodeRnm(9, 79); // eclFocusConcept
        {int[] a = {64,65,66,67,68}; op[63] = getOpcodeCat(a);}
        {char[] a = {40}; op[64] = getOpcodeTls(a);}
        op[65] = getOpcodeRnm(53, 319); // ws
        op[66] = getOpcodeRnm(0, 0); // expressionConstraint
        op[67] = getOpcodeRnm(53, 319); // ws
        {char[] a = {41}; op[68] = getOpcodeTls(a);}
        {int[] a = {70,74,78}; op[69] = getOpcodeCat(a);}
        op[70] = getOpcodeRep((char)0, (char)1, 71);
        {int[] a = {72,73}; op[71] = getOpcodeCat(a);}
        op[72] = getOpcodeRnm(16, 105); // constraintOperator
        op[73] = getOpcodeRnm(53, 319); // ws
        op[74] = getOpcodeRep((char)0, (char)1, 75);
        {int[] a = {76,77}; op[75] = getOpcodeCat(a);}
        op[76] = getOpcodeRnm(11, 83); // memberOf
        op[77] = getOpcodeRnm(53, 319); // ws
        op[78] = getOpcodeRnm(9, 79); // eclFocusConcept
        {int[] a = {80,81}; op[79] = getOpcodeAlt(a);}
        op[80] = getOpcodeRnm(12, 84); // conceptReference
        op[81] = getOpcodeRnm(15, 104); // wildCard
        {char[] a = {46}; op[82] = getOpcodeTls(a);}
        {char[] a = {94}; op[83] = getOpcodeTls(a);}
        {int[] a = {85,86}; op[84] = getOpcodeCat(a);}
        op[85] = getOpcodeRnm(13, 94); // conceptId
        op[86] = getOpcodeRep((char)0, (char)1, 87);
        {int[] a = {88,89,90,91,92,93}; op[87] = getOpcodeCat(a);}
        op[88] = getOpcodeRnm(53, 319); // ws
        {char[] a = {124}; op[89] = getOpcodeTls(a);}
        op[90] = getOpcodeRnm(53, 319); // ws
        op[91] = getOpcodeRnm(14, 95); // term
        op[92] = getOpcodeRnm(53, 319); // ws
        {char[] a = {124}; op[93] = getOpcodeTls(a);}
        op[94] = getOpcodeRnm(52, 315); // sctId
        {int[] a = {96,98}; op[95] = getOpcodeCat(a);}
        op[96] = getOpcodeRep((char)1, Character.MAX_VALUE, 97);
        op[97] = getOpcodeRnm(68, 372); // nonwsNonPipe
        op[98] = getOpcodeRep((char)0, Character.MAX_VALUE, 99);
        {int[] a = {100,102}; op[99] = getOpcodeCat(a);}
        op[100] = getOpcodeRep((char)1, Character.MAX_VALUE, 101);
        op[101] = getOpcodeRnm(59, 363); // SP
        op[102] = getOpcodeRep((char)1, Character.MAX_VALUE, 103);
        op[103] = getOpcodeRnm(68, 372); // nonwsNonPipe
        {char[] a = {42}; op[104] = getOpcodeTls(a);}
        {int[] a = {106,107,108,109,110,111}; op[105] = getOpcodeAlt(a);}
        op[106] = getOpcodeRnm(19, 114); // childOf
        op[107] = getOpcodeRnm(18, 113); // descendantOrSelfOf
        op[108] = getOpcodeRnm(17, 112); // descendantOf
        op[109] = getOpcodeRnm(22, 117); // parentOf
        op[110] = getOpcodeRnm(21, 116); // ancestorOrSelfOf
        op[111] = getOpcodeRnm(20, 115); // ancestorOf
        {char[] a = {60}; op[112] = getOpcodeTls(a);}
        {char[] a = {60,60}; op[113] = getOpcodeTls(a);}
        {char[] a = {60,33}; op[114] = getOpcodeTls(a);}
        {char[] a = {62}; op[115] = getOpcodeTls(a);}
        {char[] a = {62,62}; op[116] = getOpcodeTls(a);}
        {char[] a = {62,33}; op[117] = getOpcodeTls(a);}
        {int[] a = {119,130}; op[118] = getOpcodeAlt(a);}
        {int[] a = {120,123,126,129}; op[119] = getOpcodeCat(a);}
        {int[] a = {121,122}; op[120] = getOpcodeAlt(a);}
        {char[] a = {97}; op[121] = getOpcodeTls(a);}
        {char[] a = {65}; op[122] = getOpcodeTls(a);}
        {int[] a = {124,125}; op[123] = getOpcodeAlt(a);}
        {char[] a = {110}; op[124] = getOpcodeTls(a);}
        {char[] a = {78}; op[125] = getOpcodeTls(a);}
        {int[] a = {127,128}; op[126] = getOpcodeAlt(a);}
        {char[] a = {100}; op[127] = getOpcodeTls(a);}
        {char[] a = {68}; op[128] = getOpcodeTls(a);}
        op[129] = getOpcodeRnm(54, 326); // mws
        {char[] a = {44}; op[130] = getOpcodeTls(a);}
        {int[] a = {132,135,138}; op[131] = getOpcodeCat(a);}
        {int[] a = {133,134}; op[132] = getOpcodeAlt(a);}
        {char[] a = {111}; op[133] = getOpcodeTls(a);}
        {char[] a = {79}; op[134] = getOpcodeTls(a);}
        {int[] a = {136,137}; op[135] = getOpcodeAlt(a);}
        {char[] a = {114}; op[136] = getOpcodeTls(a);}
        {char[] a = {82}; op[137] = getOpcodeTls(a);}
        op[138] = getOpcodeRnm(54, 326); // mws
        {int[] a = {140,143,146,149,152,155}; op[139] = getOpcodeCat(a);}
        {int[] a = {141,142}; op[140] = getOpcodeAlt(a);}
        {char[] a = {109}; op[141] = getOpcodeTls(a);}
        {char[] a = {77}; op[142] = getOpcodeTls(a);}
        {int[] a = {144,145}; op[143] = getOpcodeAlt(a);}
        {char[] a = {105}; op[144] = getOpcodeTls(a);}
        {char[] a = {73}; op[145] = getOpcodeTls(a);}
        {int[] a = {147,148}; op[146] = getOpcodeAlt(a);}
        {char[] a = {110}; op[147] = getOpcodeTls(a);}
        {char[] a = {78}; op[148] = getOpcodeTls(a);}
        {int[] a = {150,151}; op[149] = getOpcodeAlt(a);}
        {char[] a = {117}; op[150] = getOpcodeTls(a);}
        {char[] a = {85}; op[151] = getOpcodeTls(a);}
        {int[] a = {153,154}; op[152] = getOpcodeAlt(a);}
        {char[] a = {115}; op[153] = getOpcodeTls(a);}
        {char[] a = {83}; op[154] = getOpcodeTls(a);}
        op[155] = getOpcodeRnm(54, 326); // mws
        {int[] a = {157,158,159}; op[156] = getOpcodeCat(a);}
        op[157] = getOpcodeRnm(29, 175); // subRefinement
        op[158] = getOpcodeRnm(53, 319); // ws
        op[159] = getOpcodeRep((char)0, (char)1, 160);
        {int[] a = {161,162}; op[160] = getOpcodeAlt(a);}
        op[161] = getOpcodeRnm(27, 163); // conjunctionRefinementSet
        op[162] = getOpcodeRnm(28, 169); // disjunctionRefinementSet
        op[163] = getOpcodeRep((char)1, Character.MAX_VALUE, 164);
        {int[] a = {165,166,167,168}; op[164] = getOpcodeCat(a);}
        op[165] = getOpcodeRnm(53, 319); // ws
        op[166] = getOpcodeRnm(23, 118); // conjunction
        op[167] = getOpcodeRnm(53, 319); // ws
        op[168] = getOpcodeRnm(29, 175); // subRefinement
        op[169] = getOpcodeRep((char)1, Character.MAX_VALUE, 170);
        {int[] a = {171,172,173,174}; op[170] = getOpcodeCat(a);}
        op[171] = getOpcodeRnm(53, 319); // ws
        op[172] = getOpcodeRnm(24, 131); // disjunction
        op[173] = getOpcodeRnm(53, 319); // ws
        op[174] = getOpcodeRnm(29, 175); // subRefinement
        {int[] a = {176,177,178}; op[175] = getOpcodeAlt(a);}
        op[176] = getOpcodeRnm(30, 184); // eclAttributeSet
        op[177] = getOpcodeRnm(34, 211); // eclAttributeGroup
        {int[] a = {179,180,181,182,183}; op[178] = getOpcodeCat(a);}
        {char[] a = {40}; op[179] = getOpcodeTls(a);}
        op[180] = getOpcodeRnm(53, 319); // ws
        op[181] = getOpcodeRnm(26, 156); // eclRefinement
        op[182] = getOpcodeRnm(53, 319); // ws
        {char[] a = {41}; op[183] = getOpcodeTls(a);}
        {int[] a = {185,186,187}; op[184] = getOpcodeCat(a);}
        op[185] = getOpcodeRnm(33, 203); // subAttributeSet
        op[186] = getOpcodeRnm(53, 319); // ws
        op[187] = getOpcodeRep((char)0, (char)1, 188);
        {int[] a = {189,190}; op[188] = getOpcodeAlt(a);}
        op[189] = getOpcodeRnm(31, 191); // conjunctionAttributeSet
        op[190] = getOpcodeRnm(32, 197); // disjunctionAttributeSet
        op[191] = getOpcodeRep((char)1, Character.MAX_VALUE, 192);
        {int[] a = {193,194,195,196}; op[192] = getOpcodeCat(a);}
        op[193] = getOpcodeRnm(53, 319); // ws
        op[194] = getOpcodeRnm(23, 118); // conjunction
        op[195] = getOpcodeRnm(53, 319); // ws
        op[196] = getOpcodeRnm(33, 203); // subAttributeSet
        op[197] = getOpcodeRep((char)1, Character.MAX_VALUE, 198);
        {int[] a = {199,200,201,202}; op[198] = getOpcodeCat(a);}
        op[199] = getOpcodeRnm(53, 319); // ws
        op[200] = getOpcodeRnm(24, 131); // disjunction
        op[201] = getOpcodeRnm(53, 319); // ws
        op[202] = getOpcodeRnm(33, 203); // subAttributeSet
        {int[] a = {204,205}; op[203] = getOpcodeAlt(a);}
        op[204] = getOpcodeRnm(35, 223); // eclAttribute
        {int[] a = {206,207,208,209,210}; op[205] = getOpcodeCat(a);}
        {char[] a = {40}; op[206] = getOpcodeTls(a);}
        op[207] = getOpcodeRnm(53, 319); // ws
        op[208] = getOpcodeRnm(30, 184); // eclAttributeSet
        op[209] = getOpcodeRnm(53, 319); // ws
        {char[] a = {41}; op[210] = getOpcodeTls(a);}
        {int[] a = {212,218,219,220,221,222}; op[211] = getOpcodeCat(a);}
        op[212] = getOpcodeRep((char)0, (char)1, 213);
        {int[] a = {214,215,216,217}; op[213] = getOpcodeCat(a);}
        {char[] a = {91}; op[214] = getOpcodeTls(a);}
        op[215] = getOpcodeRnm(36, 256); // cardinality
        {char[] a = {93}; op[216] = getOpcodeTls(a);}
        op[217] = getOpcodeRnm(53, 319); // ws
        {char[] a = {123}; op[218] = getOpcodeTls(a);}
        op[219] = getOpcodeRnm(53, 319); // ws
        op[220] = getOpcodeRnm(30, 184); // eclAttributeSet
        op[221] = getOpcodeRnm(53, 319); // ws
        {char[] a = {125}; op[222] = getOpcodeTls(a);}
        {int[] a = {224,230,234,238,239,240}; op[223] = getOpcodeCat(a);}
        op[224] = getOpcodeRep((char)0, (char)1, 225);
        {int[] a = {226,227,228,229}; op[225] = getOpcodeCat(a);}
        {char[] a = {91}; op[226] = getOpcodeTls(a);}
        op[227] = getOpcodeRnm(36, 256); // cardinality
        {char[] a = {93}; op[228] = getOpcodeTls(a);}
        op[229] = getOpcodeRnm(53, 319); // ws
        op[230] = getOpcodeRep((char)0, (char)1, 231);
        {int[] a = {232,233}; op[231] = getOpcodeCat(a);}
        op[232] = getOpcodeRnm(41, 266); // reverseFlag
        op[233] = getOpcodeRnm(53, 319); // ws
        op[234] = getOpcodeRep((char)0, (char)1, 235);
        {int[] a = {236,237}; op[235] = getOpcodeCat(a);}
        op[236] = getOpcodeRnm(42, 267); // attributeOperator
        op[237] = getOpcodeRnm(53, 319); // ws
        op[238] = getOpcodeRnm(43, 270); // eclAttributeName
        op[239] = getOpcodeRnm(53, 319); // ws
        {int[] a = {241,245,250}; op[240] = getOpcodeAlt(a);}
        {int[] a = {242,243,244}; op[241] = getOpcodeCat(a);}
        op[242] = getOpcodeRnm(44, 273); // expressionComparisonOperator
        op[243] = getOpcodeRnm(53, 319); // ws
        op[244] = getOpcodeRnm(7, 52); // subExpressionConstraint
        {int[] a = {246,247,248,249}; op[245] = getOpcodeCat(a);}
        op[246] = getOpcodeRnm(45, 276); // numericComparisonOperator
        op[247] = getOpcodeRnm(53, 319); // ws
        {char[] a = {35}; op[248] = getOpcodeTls(a);}
        op[249] = getOpcodeRnm(47, 286); // numericValue
        {int[] a = {251,252,253,254,255}; op[250] = getOpcodeCat(a);}
        op[251] = getOpcodeRnm(46, 283); // stringComparisonOperator
        op[252] = getOpcodeRnm(53, 319); // ws
        op[253] = getOpcodeRnm(63, 367); // QM
        op[254] = getOpcodeRnm(48, 294); // stringValue
        op[255] = getOpcodeRnm(63, 367); // QM
        {int[] a = {257,258,259}; op[256] = getOpcodeCat(a);}
        op[257] = getOpcodeRnm(37, 260); // minValue
        op[258] = getOpcodeRnm(38, 261); // to
        op[259] = getOpcodeRnm(39, 262); // maxValue
        op[260] = getOpcodeRnm(51, 309); // nonNegativeIntegerValue
        {char[] a = {46,46}; op[261] = getOpcodeTls(a);}
        {int[] a = {263,264}; op[262] = getOpcodeAlt(a);}
        op[263] = getOpcodeRnm(51, 309); // nonNegativeIntegerValue
        op[264] = getOpcodeRnm(40, 265); // many
        {char[] a = {42}; op[265] = getOpcodeTls(a);}
        {char[] a = {82}; op[266] = getOpcodeTls(a);}
        {int[] a = {268,269}; op[267] = getOpcodeAlt(a);}
        op[268] = getOpcodeRnm(18, 113); // descendantOrSelfOf
        op[269] = getOpcodeRnm(17, 112); // descendantOf
        {int[] a = {271,272}; op[270] = getOpcodeAlt(a);}
        op[271] = getOpcodeRnm(12, 84); // conceptReference
        op[272] = getOpcodeRnm(15, 104); // wildCard
        {int[] a = {274,275}; op[273] = getOpcodeAlt(a);}
        {char[] a = {61}; op[274] = getOpcodeTls(a);}
        {char[] a = {33,61}; op[275] = getOpcodeTls(a);}
        {int[] a = {277,278,279,280,281,282}; op[276] = getOpcodeAlt(a);}
        {char[] a = {61}; op[277] = getOpcodeTls(a);}
        {char[] a = {33,61}; op[278] = getOpcodeTls(a);}
        {char[] a = {60,61}; op[279] = getOpcodeTls(a);}
        {char[] a = {60}; op[280] = getOpcodeTls(a);}
        {char[] a = {62,61}; op[281] = getOpcodeTls(a);}
        {char[] a = {62}; op[282] = getOpcodeTls(a);}
        {int[] a = {284,285}; op[283] = getOpcodeAlt(a);}
        {char[] a = {61}; op[284] = getOpcodeTls(a);}
        {char[] a = {33,61}; op[285] = getOpcodeTls(a);}
        {int[] a = {287,291}; op[286] = getOpcodeCat(a);}
        op[287] = getOpcodeRep((char)0, (char)1, 288);
        {int[] a = {289,290}; op[288] = getOpcodeAlt(a);}
        {char[] a = {45}; op[289] = getOpcodeTls(a);}
        {char[] a = {43}; op[290] = getOpcodeTls(a);}
        {int[] a = {292,293}; op[291] = getOpcodeAlt(a);}
        op[292] = getOpcodeRnm(50, 304); // decimalValue
        op[293] = getOpcodeRnm(49, 298); // integerValue
        op[294] = getOpcodeRep((char)1, Character.MAX_VALUE, 295);
        {int[] a = {296,297}; op[295] = getOpcodeAlt(a);}
        op[296] = getOpcodeRnm(69, 378); // anyNonEscapedChar
        op[297] = getOpcodeRnm(70, 389); // escapedChar
        {int[] a = {299,303}; op[298] = getOpcodeAlt(a);}
        {int[] a = {300,301}; op[299] = getOpcodeCat(a);}
        op[300] = getOpcodeRnm(67, 371); // digitNonZero
        op[301] = getOpcodeRep((char)0, Character.MAX_VALUE, 302);
        op[302] = getOpcodeRnm(65, 369); // digit
        op[303] = getOpcodeRnm(66, 370); // zero
        {int[] a = {305,306,307}; op[304] = getOpcodeCat(a);}
        op[305] = getOpcodeRnm(49, 298); // integerValue
        {char[] a = {46}; op[306] = getOpcodeTls(a);}
        op[307] = getOpcodeRep((char)1, Character.MAX_VALUE, 308);
        op[308] = getOpcodeRnm(65, 369); // digit
        {int[] a = {310,314}; op[309] = getOpcodeAlt(a);}
        {int[] a = {311,312}; op[310] = getOpcodeCat(a);}
        op[311] = getOpcodeRnm(67, 371); // digitNonZero
        op[312] = getOpcodeRep((char)0, Character.MAX_VALUE, 313);
        op[313] = getOpcodeRnm(65, 369); // digit
        op[314] = getOpcodeRnm(66, 370); // zero
        {int[] a = {316,317}; op[315] = getOpcodeCat(a);}
        op[316] = getOpcodeRnm(67, 371); // digitNonZero
        op[317] = getOpcodeRep((char)5, (char)17, 318);
        op[318] = getOpcodeRnm(65, 369); // digit
        op[319] = getOpcodeRep((char)0, Character.MAX_VALUE, 320);
        {int[] a = {321,322,323,324,325}; op[320] = getOpcodeAlt(a);}
        op[321] = getOpcodeRnm(59, 363); // SP
        op[322] = getOpcodeRnm(60, 364); // HTAB
        op[323] = getOpcodeRnm(61, 365); // CR
        op[324] = getOpcodeRnm(62, 366); // LF
        op[325] = getOpcodeRnm(55, 333); // comment
        op[326] = getOpcodeRep((char)1, Character.MAX_VALUE, 327);
        {int[] a = {328,329,330,331,332}; op[327] = getOpcodeAlt(a);}
        op[328] = getOpcodeRnm(59, 363); // SP
        op[329] = getOpcodeRnm(60, 364); // HTAB
        op[330] = getOpcodeRnm(61, 365); // CR
        op[331] = getOpcodeRnm(62, 366); // LF
        op[332] = getOpcodeRnm(55, 333); // comment
        {int[] a = {334,335,339}; op[333] = getOpcodeCat(a);}
        {char[] a = {47,42}; op[334] = getOpcodeTls(a);}
        op[335] = getOpcodeRep((char)0, Character.MAX_VALUE, 336);
        {int[] a = {337,338}; op[336] = getOpcodeAlt(a);}
        op[337] = getOpcodeRnm(56, 340); // nonStarChar
        op[338] = getOpcodeRnm(57, 350); // starWithNonFSlash
        {char[] a = {42,47}; op[339] = getOpcodeTls(a);}
        {int[] a = {341,342,343,344,345,346,347,348,349}; op[340] = getOpcodeAlt(a);}
        op[341] = getOpcodeRnm(59, 363); // SP
        op[342] = getOpcodeRnm(60, 364); // HTAB
        op[343] = getOpcodeRnm(61, 365); // CR
        op[344] = getOpcodeRnm(62, 366); // LF
        op[345] = getOpcodeTrg((char)33, (char)41);
        op[346] = getOpcodeTrg((char)43, (char)126);
        op[347] = getOpcodeRnm(71, 396); // UTF8-2
        op[348] = getOpcodeRnm(72, 399); // UTF8-3
        op[349] = getOpcodeRnm(73, 416); // UTF8-4
        {int[] a = {351,352}; op[350] = getOpcodeCat(a);}
        {char[] a = {42}; op[351] = getOpcodeTbs(a);}
        op[352] = getOpcodeRnm(58, 353); // nonFSlash
        {int[] a = {354,355,356,357,358,359,360,361,362}; op[353] = getOpcodeAlt(a);}
        op[354] = getOpcodeRnm(59, 363); // SP
        op[355] = getOpcodeRnm(60, 364); // HTAB
        op[356] = getOpcodeRnm(61, 365); // CR
        op[357] = getOpcodeRnm(62, 366); // LF
        op[358] = getOpcodeTrg((char)33, (char)46);
        op[359] = getOpcodeTrg((char)48, (char)126);
        op[360] = getOpcodeRnm(71, 396); // UTF8-2
        op[361] = getOpcodeRnm(72, 399); // UTF8-3
        op[362] = getOpcodeRnm(73, 416); // UTF8-4
        {char[] a = {32}; op[363] = getOpcodeTbs(a);}
        {char[] a = {9}; op[364] = getOpcodeTbs(a);}
        {char[] a = {13}; op[365] = getOpcodeTbs(a);}
        {char[] a = {10}; op[366] = getOpcodeTbs(a);}
        {char[] a = {34}; op[367] = getOpcodeTbs(a);}
        {char[] a = {92}; op[368] = getOpcodeTbs(a);}
        op[369] = getOpcodeTrg((char)48, (char)57);
        {char[] a = {48}; op[370] = getOpcodeTbs(a);}
        op[371] = getOpcodeTrg((char)49, (char)57);
        {int[] a = {373,374,375,376,377}; op[372] = getOpcodeAlt(a);}
        op[373] = getOpcodeTrg((char)33, (char)123);
        op[374] = getOpcodeTrg((char)125, (char)126);
        op[375] = getOpcodeRnm(71, 396); // UTF8-2
        op[376] = getOpcodeRnm(72, 399); // UTF8-3
        op[377] = getOpcodeRnm(73, 416); // UTF8-4
        {int[] a = {379,380,381,382,383,384,385,386,387,388}; op[378] = getOpcodeAlt(a);}
        op[379] = getOpcodeRnm(59, 363); // SP
        op[380] = getOpcodeRnm(60, 364); // HTAB
        op[381] = getOpcodeRnm(61, 365); // CR
        op[382] = getOpcodeRnm(62, 366); // LF
        op[383] = getOpcodeTrg((char)32, (char)33);
        op[384] = getOpcodeTrg((char)35, (char)91);
        op[385] = getOpcodeTrg((char)93, (char)126);
        op[386] = getOpcodeRnm(71, 396); // UTF8-2
        op[387] = getOpcodeRnm(72, 399); // UTF8-3
        op[388] = getOpcodeRnm(73, 416); // UTF8-4
        {int[] a = {390,393}; op[389] = getOpcodeAlt(a);}
        {int[] a = {391,392}; op[390] = getOpcodeCat(a);}
        op[391] = getOpcodeRnm(64, 368); // BS
        op[392] = getOpcodeRnm(63, 367); // QM
        {int[] a = {394,395}; op[393] = getOpcodeCat(a);}
        op[394] = getOpcodeRnm(64, 368); // BS
        op[395] = getOpcodeRnm(64, 368); // BS
        {int[] a = {397,398}; op[396] = getOpcodeCat(a);}
        op[397] = getOpcodeTrg((char)194, (char)223);
        op[398] = getOpcodeRnm(74, 431); // UTF8-tail
        {int[] a = {400,404,408,412}; op[399] = getOpcodeAlt(a);}
        {int[] a = {401,402,403}; op[400] = getOpcodeCat(a);}
        {char[] a = {224}; op[401] = getOpcodeTbs(a);}
        op[402] = getOpcodeTrg((char)160, (char)191);
        op[403] = getOpcodeRnm(74, 431); // UTF8-tail
        {int[] a = {405,406}; op[404] = getOpcodeCat(a);}
        op[405] = getOpcodeTrg((char)225, (char)236);
        op[406] = getOpcodeRep((char)2, (char)2, 407);
        op[407] = getOpcodeRnm(74, 431); // UTF8-tail
        {int[] a = {409,410,411}; op[408] = getOpcodeCat(a);}
        {char[] a = {237}; op[409] = getOpcodeTbs(a);}
        op[410] = getOpcodeTrg((char)128, (char)159);
        op[411] = getOpcodeRnm(74, 431); // UTF8-tail
        {int[] a = {413,414}; op[412] = getOpcodeCat(a);}
        op[413] = getOpcodeTrg((char)238, (char)239);
        op[414] = getOpcodeRep((char)2, (char)2, 415);
        op[415] = getOpcodeRnm(74, 431); // UTF8-tail
        {int[] a = {417,422,426}; op[416] = getOpcodeAlt(a);}
        {int[] a = {418,419,420}; op[417] = getOpcodeCat(a);}
        {char[] a = {240}; op[418] = getOpcodeTbs(a);}
        op[419] = getOpcodeTrg((char)144, (char)191);
        op[420] = getOpcodeRep((char)2, (char)2, 421);
        op[421] = getOpcodeRnm(74, 431); // UTF8-tail
        {int[] a = {423,424}; op[422] = getOpcodeCat(a);}
        op[423] = getOpcodeTrg((char)241, (char)243);
        op[424] = getOpcodeRep((char)3, (char)3, 425);
        op[425] = getOpcodeRnm(74, 431); // UTF8-tail
        {int[] a = {427,428,429}; op[426] = getOpcodeCat(a);}
        {char[] a = {244}; op[427] = getOpcodeTbs(a);}
        op[428] = getOpcodeTrg((char)128, (char)143);
        op[429] = getOpcodeRep((char)2, (char)2, 430);
        op[430] = getOpcodeRnm(74, 431); // UTF8-tail
        op[431] = getOpcodeTrg((char)128, (char)191);
        return op;
    }

    public static void display(PrintStream out){
        out.println(";");
        out.println("; package.name.ECLParser");
        out.println(";");
        out.println("expressionConstraint = ws ( refinedExpressionConstraint / compoundExpressionConstraint / dottedExpressionConstraint / subExpressionConstraint ) ws");
        out.println("refinedExpressionConstraint = simpleExpressionConstraint ws \":\" ws eclRefinement");
        out.println("compoundExpressionConstraint = conjunctionExpressionConstraint / disjunctionExpressionConstraint / exclusionExpressionConstraint");
        out.println("conjunctionExpressionConstraint = subExpressionConstraint 1*(ws conjunction ws subExpressionConstraint)");
        out.println("disjunctionExpressionConstraint = subExpressionConstraint 1*(ws disjunction ws subExpressionConstraint)");
        out.println("exclusionExpressionConstraint = subExpressionConstraint ws exclusion ws subExpressionConstraint");
        out.println("dottedExpressionConstraint = subExpressionConstraint 1*(ws dot ws [attributeOperator ws] eclAttributeName)");
        out.println("subExpressionConstraint = [constraintOperator ws] [memberOf ws] (eclFocusConcept / \"(\" ws expressionConstraint ws \")\")");
        out.println("simpleExpressionConstraint = [constraintOperator ws] [memberOf ws] eclFocusConcept");
        out.println("eclFocusConcept = conceptReference / wildCard");
        out.println("dot = \".\"");
        out.println("memberOf = \"^\"");
        out.println("conceptReference = conceptId [ws \"|\" ws term ws \"|\"]");
        out.println("conceptId = sctId");
        out.println("term = 1*nonwsNonPipe *( 1*SP 1*nonwsNonPipe )");
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
        out.println("eclAttribute = [\"[\" cardinality \"]\" ws] [reverseFlag ws] [attributeOperator ws] eclAttributeName ws (expressionComparisonOperator ws subExpressionConstraint / numericComparisonOperator ws \"#\" numericValue / stringComparisonOperator ws QM stringValue QM)");
        out.println("cardinality = minValue to maxValue");
        out.println("minValue = nonNegativeIntegerValue");
        out.println("to = \"..\"");
        out.println("maxValue = nonNegativeIntegerValue / many");
        out.println("many = \"*\"");
        out.println("reverseFlag = \"R\"");
        out.println("attributeOperator = descendantOrSelfOf / descendantOf");
        out.println("eclAttributeName = conceptReference / wildCard");
        out.println("expressionComparisonOperator = \"=\" / \"!=\"");
        out.println("numericComparisonOperator = \"=\" / \"!=\" / \"<=\" / \"<\" / \">=\" / \">\"");
        out.println("stringComparisonOperator = \"=\" / \"!=\"");
        out.println("numericValue = [\"-\"/\"+\"] (decimalValue / integerValue)");
        out.println("stringValue = 1*(anyNonEscapedChar / escapedChar)");
        out.println("integerValue =  digitNonZero *digit / zero");
        out.println("decimalValue = integerValue \".\" 1*digit");
        out.println("nonNegativeIntegerValue = (digitNonZero *digit ) / zero");
        out.println("sctId = digitNonZero 5*17( digit )");
        out.println("ws = *( SP / HTAB / CR / LF / comment ) ; optional white space");
        out.println("mws = 1*( SP / HTAB / CR / LF / comment ) ; mandatory white space");
        out.println("comment = \"/*\" *(nonStarChar / starWithNonFSlash) \"*/\"");
        out.println("nonStarChar = SP / HTAB / CR / LF / %x21-29 / %x2B-7E /UTF8-2 / UTF8-3 / UTF8-4");
        out.println("starWithNonFSlash = %x2A nonFSlash");
        out.println("nonFSlash = SP / HTAB / CR / LF / %x21-2E / %x30-7E /UTF8-2 / UTF8-3 / UTF8-4");
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
        out.println("anyNonEscapedChar = SP / HTAB / CR / LF / %x20-21 / %x23-5B / %x5D-7E / UTF8-2 / UTF8-3 / UTF8-4");
        out.println("escapedChar = BS QM / BS BS");
        out.println("UTF8-2 = %xC2-DF UTF8-tail");
        out.println("UTF8-3 = %xE0 %xA0-BF UTF8-tail / %xE1-EC 2( UTF8-tail ) / %xED %x80-9F UTF8-tail / %xEE-EF 2( UTF8-tail )");
        out.println("UTF8-4 = %xF0 %x90-BF 2( UTF8-tail ) / %xF1-F3 3( UTF8-tail ) / %xF4 %x80-8F 2( UTF8-tail )");
        out.println("UTF8-tail = %x80-BF");
    }
}
