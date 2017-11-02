package org.ihtsdo.rvf.util;

import apg.Grammar;
import java.io.PrintStream;

public class LongECLGrammar extends Grammar{

    // public API
    public static Grammar getInstance(){
        if(factoryInstance == null){
            factoryInstance = new LongECLGrammar(getRules(), getUdts(), getOpcodes());
        }
        return factoryInstance;
    }

    // rule name enum
    public static int ruleCount = 74;
    public enum RuleNames{
        ANCESTOROF("ancestorOf", 20, 260, 34),
        ANCESTORORSELFOF("ancestorOrSelfOf", 21, 294, 52),
        ANYNONESCAPEDCHAR("anyNonEscapedChar", 68, 717, 11),
        BS("BS", 63, 707, 1),
        CARDINALITY("cardinality", 36, 508, 4),
        CHILDOF("childOf", 19, 235, 25),
        COMMENT("comment", 54, 672, 7),
        COMPOUNDEXPRESSIONCONSTRAINT("compoundExpressionConstraint", 2, 14, 4),
        CONCEPTID("conceptId", 13, 108, 1),
        CONJUNCTION("conjunction", 23, 374, 13),
        CONJUNCTIONATTRIBUTESET("conjunctionAttributeSet", 31, 447, 6),
        CONJUNCTIONEXPRESSIONCONSTRAINT("conjunctionExpressionConstraint", 3, 18, 8),
        CONJUNCTIONREFINEMENTSET("conjunctionRefinementSet", 27, 419, 6),
        CONSTRAINTOPERATOR("constraintOperator", 16, 130, 7),
        CR("CR", 60, 704, 1),
        DECIMALVALUE("decimalValue", 49, 643, 5),
        DESCENDANTOF("descendantOf", 17, 137, 40),
        DESCENDANTORSELFOF("descendantOrSelfOf", 18, 177, 58),
        DIGIT("digit", 64, 708, 1),
        DIGITNONZERO("digitNonZero", 66, 710, 1),
        DISJUNCTION("disjunction", 24, 387, 8),
        DISJUNCTIONATTRIBUTESET("disjunctionAttributeSet", 32, 453, 6),
        DISJUNCTIONEXPRESSIONCONSTRAINT("disjunctionExpressionConstraint", 4, 26, 8),
        DISJUNCTIONREFINEMENTSET("disjunctionRefinementSet", 28, 425, 6),
        DOT("dot", 10, 70, 1),
        DOTTEDEXPRESSIONATTRIBUTE("dottedExpressionAttribute", 7, 46, 4),
        DOTTEDEXPRESSIONCONSTRAINT("dottedExpressionConstraint", 6, 40, 6),
        ECLATTRIBUTE("eclAttribute", 35, 479, 29),
        ECLATTRIBUTEGROUP("eclAttributeGroup", 34, 467, 12),
        ECLATTRIBUTENAME("eclAttributeName", 42, 572, 1),
        ECLATTRIBUTESET("eclAttributeSet", 30, 440, 7),
        ECLCONCEPTREFERENCE("eclConceptReference", 12, 98, 10),
        ECLFOCUSCONCEPT("eclFocusConcept", 9, 67, 3),
        ECLREFINEMENT("eclRefinement", 26, 412, 7),
        ESCAPEDCHAR("escapedChar", 69, 728, 7),
        EXCLUSION("exclusion", 25, 395, 17),
        EXCLUSIONEXPRESSIONCONSTRAINT("exclusionExpressionConstraint", 5, 34, 6),
        EXPRESSIONCOMPARISONOPERATOR("expressionComparisonOperator", 43, 573, 16),
        EXPRESSIONCONSTRAINT("expressionConstraint", 0, 0, 8),
        HTAB("HTAB", 59, 703, 1),
        INTEGERVALUE("integerValue", 48, 637, 6),
        LF("LF", 61, 705, 1),
        MANY("many", 40, 527, 15),
        MAXVALUE("maxValue", 39, 524, 3),
        MEMBEROF("memberOf", 11, 71, 27),
        MINVALUE("minValue", 37, 512, 1),
        MWS("mws", 53, 665, 7),
        NONFSLASH("nonFSlash", 57, 692, 10),
        NONNEGATIVEINTEGERVALUE("nonNegativeIntegerValue", 50, 648, 6),
        NONSTARCHAR("nonStarChar", 55, 679, 10),
        NONWSNONPIPE("nonwsNonPipe", 67, 711, 6),
        NUMERICCOMPARISONOPERATOR("numericComparisonOperator", 44, 589, 20),
        NUMERICVALUE("numericValue", 46, 625, 8),
        PARENTOF("parentOf", 22, 346, 28),
        QM("QM", 62, 706, 1),
        REFINEDEXPRESSIONCONSTRAINT("refinedExpressionConstraint", 1, 8, 6),
        REVERSEFLAG("reverseFlag", 41, 542, 30),
        SCTID("sctId", 51, 654, 4),
        SP("SP", 58, 702, 1),
        STARWITHNONFSLASH("starWithNonFSlash", 56, 689, 3),
        STRINGCOMPARISONOPERATOR("stringComparisonOperator", 45, 609, 16),
        STRINGVALUE("stringValue", 47, 633, 4),
        SUBATTRIBUTESET("subAttributeSet", 33, 459, 8),
        SUBEXPRESSIONCONSTRAINT("subExpressionConstraint", 8, 50, 17),
        SUBREFINEMENT("subRefinement", 29, 431, 9),
        TERM("term", 14, 109, 9),
        TO("to", 38, 513, 11),
        UTF8_2("UTF8-2", 70, 735, 3),
        UTF8_3("UTF8-3", 71, 738, 17),
        UTF8_4("UTF8-4", 72, 755, 15),
        UTF8_TAIL("UTF8-tail", 73, 770, 1),
        WILDCARD("wildCard", 15, 118, 12),
        WS("ws", 52, 658, 7),
        ZERO("zero", 65, 709, 1);
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
    private static LongECLGrammar factoryInstance = null;
    private LongECLGrammar(Rule[] rules, Udt[] udts, Opcode[] opcodes){
        super(rules, udts, opcodes);
    }

    private static Rule[] getRules(){
    	Rule[] rules = new Rule[74];
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
    	Opcode[] op = new Opcode[771];
        {int[] a = {1,2,7}; op[0] = getOpcodeCat(a);}
        op[1] = getOpcodeRnm(52, 658); // ws
        {int[] a = {3,4,5,6}; op[2] = getOpcodeAlt(a);}
        op[3] = getOpcodeRnm(1, 8); // refinedExpressionConstraint
        op[4] = getOpcodeRnm(2, 14); // compoundExpressionConstraint
        op[5] = getOpcodeRnm(6, 40); // dottedExpressionConstraint
        op[6] = getOpcodeRnm(8, 50); // subExpressionConstraint
        op[7] = getOpcodeRnm(52, 658); // ws
        {int[] a = {9,10,11,12,13}; op[8] = getOpcodeCat(a);}
        op[9] = getOpcodeRnm(8, 50); // subExpressionConstraint
        op[10] = getOpcodeRnm(52, 658); // ws
        {char[] a = {58}; op[11] = getOpcodeTls(a);}
        op[12] = getOpcodeRnm(52, 658); // ws
        op[13] = getOpcodeRnm(26, 412); // eclRefinement
        {int[] a = {15,16,17}; op[14] = getOpcodeAlt(a);}
        op[15] = getOpcodeRnm(3, 18); // conjunctionExpressionConstraint
        op[16] = getOpcodeRnm(4, 26); // disjunctionExpressionConstraint
        op[17] = getOpcodeRnm(5, 34); // exclusionExpressionConstraint
        {int[] a = {19,20}; op[18] = getOpcodeCat(a);}
        op[19] = getOpcodeRnm(8, 50); // subExpressionConstraint
        op[20] = getOpcodeRep((char)1, Character.MAX_VALUE, 21);
        {int[] a = {22,23,24,25}; op[21] = getOpcodeCat(a);}
        op[22] = getOpcodeRnm(52, 658); // ws
        op[23] = getOpcodeRnm(23, 374); // conjunction
        op[24] = getOpcodeRnm(52, 658); // ws
        op[25] = getOpcodeRnm(8, 50); // subExpressionConstraint
        {int[] a = {27,28}; op[26] = getOpcodeCat(a);}
        op[27] = getOpcodeRnm(8, 50); // subExpressionConstraint
        op[28] = getOpcodeRep((char)1, Character.MAX_VALUE, 29);
        {int[] a = {30,31,32,33}; op[29] = getOpcodeCat(a);}
        op[30] = getOpcodeRnm(52, 658); // ws
        op[31] = getOpcodeRnm(24, 387); // disjunction
        op[32] = getOpcodeRnm(52, 658); // ws
        op[33] = getOpcodeRnm(8, 50); // subExpressionConstraint
        {int[] a = {35,36,37,38,39}; op[34] = getOpcodeCat(a);}
        op[35] = getOpcodeRnm(8, 50); // subExpressionConstraint
        op[36] = getOpcodeRnm(52, 658); // ws
        op[37] = getOpcodeRnm(25, 395); // exclusion
        op[38] = getOpcodeRnm(52, 658); // ws
        op[39] = getOpcodeRnm(8, 50); // subExpressionConstraint
        {int[] a = {41,42}; op[40] = getOpcodeCat(a);}
        op[41] = getOpcodeRnm(8, 50); // subExpressionConstraint
        op[42] = getOpcodeRep((char)1, Character.MAX_VALUE, 43);
        {int[] a = {44,45}; op[43] = getOpcodeCat(a);}
        op[44] = getOpcodeRnm(52, 658); // ws
        op[45] = getOpcodeRnm(7, 46); // dottedExpressionAttribute
        {int[] a = {47,48,49}; op[46] = getOpcodeCat(a);}
        op[47] = getOpcodeRnm(10, 70); // dot
        op[48] = getOpcodeRnm(52, 658); // ws
        op[49] = getOpcodeRnm(42, 572); // eclAttributeName
        {int[] a = {51,55,59}; op[50] = getOpcodeCat(a);}
        op[51] = getOpcodeRep((char)0, (char)1, 52);
        {int[] a = {53,54}; op[52] = getOpcodeCat(a);}
        op[53] = getOpcodeRnm(16, 130); // constraintOperator
        op[54] = getOpcodeRnm(52, 658); // ws
        op[55] = getOpcodeRep((char)0, (char)1, 56);
        {int[] a = {57,58}; op[56] = getOpcodeCat(a);}
        op[57] = getOpcodeRnm(11, 71); // memberOf
        op[58] = getOpcodeRnm(52, 658); // ws
        {int[] a = {60,61}; op[59] = getOpcodeAlt(a);}
        op[60] = getOpcodeRnm(9, 67); // eclFocusConcept
        {int[] a = {62,63,64,65,66}; op[61] = getOpcodeCat(a);}
        {char[] a = {40}; op[62] = getOpcodeTls(a);}
        op[63] = getOpcodeRnm(52, 658); // ws
        op[64] = getOpcodeRnm(0, 0); // expressionConstraint
        op[65] = getOpcodeRnm(52, 658); // ws
        {char[] a = {41}; op[66] = getOpcodeTls(a);}
        {int[] a = {68,69}; op[67] = getOpcodeAlt(a);}
        op[68] = getOpcodeRnm(12, 98); // eclConceptReference
        op[69] = getOpcodeRnm(15, 118); // wildCard
        {char[] a = {46}; op[70] = getOpcodeTls(a);}
        {int[] a = {72,73}; op[71] = getOpcodeAlt(a);}
        {char[] a = {94}; op[72] = getOpcodeTls(a);}
        {int[] a = {74,77,80,83,86,89,92,95}; op[73] = getOpcodeCat(a);}
        {int[] a = {75,76}; op[74] = getOpcodeAlt(a);}
        {char[] a = {109}; op[75] = getOpcodeTls(a);}
        {char[] a = {77}; op[76] = getOpcodeTls(a);}
        {int[] a = {78,79}; op[77] = getOpcodeAlt(a);}
        {char[] a = {101}; op[78] = getOpcodeTls(a);}
        {char[] a = {69}; op[79] = getOpcodeTls(a);}
        {int[] a = {81,82}; op[80] = getOpcodeAlt(a);}
        {char[] a = {109}; op[81] = getOpcodeTls(a);}
        {char[] a = {77}; op[82] = getOpcodeTls(a);}
        {int[] a = {84,85}; op[83] = getOpcodeAlt(a);}
        {char[] a = {98}; op[84] = getOpcodeTls(a);}
        {char[] a = {66}; op[85] = getOpcodeTls(a);}
        {int[] a = {87,88}; op[86] = getOpcodeAlt(a);}
        {char[] a = {101}; op[87] = getOpcodeTls(a);}
        {char[] a = {69}; op[88] = getOpcodeTls(a);}
        {int[] a = {90,91}; op[89] = getOpcodeAlt(a);}
        {char[] a = {114}; op[90] = getOpcodeTls(a);}
        {char[] a = {82}; op[91] = getOpcodeTls(a);}
        {int[] a = {93,94}; op[92] = getOpcodeAlt(a);}
        {char[] a = {111}; op[93] = getOpcodeTls(a);}
        {char[] a = {79}; op[94] = getOpcodeTls(a);}
        {int[] a = {96,97}; op[95] = getOpcodeAlt(a);}
        {char[] a = {102}; op[96] = getOpcodeTls(a);}
        {char[] a = {70}; op[97] = getOpcodeTls(a);}
        {int[] a = {99,100}; op[98] = getOpcodeCat(a);}
        op[99] = getOpcodeRnm(13, 108); // conceptId
        op[100] = getOpcodeRep((char)0, (char)1, 101);
        {int[] a = {102,103,104,105,106,107}; op[101] = getOpcodeCat(a);}
        op[102] = getOpcodeRnm(52, 658); // ws
        {char[] a = {124}; op[103] = getOpcodeTls(a);}
        op[104] = getOpcodeRnm(52, 658); // ws
        op[105] = getOpcodeRnm(14, 109); // term
        op[106] = getOpcodeRnm(52, 658); // ws
        {char[] a = {124}; op[107] = getOpcodeTls(a);}
        op[108] = getOpcodeRnm(51, 654); // sctId
        {int[] a = {110,112}; op[109] = getOpcodeCat(a);}
        op[110] = getOpcodeRep((char)1, Character.MAX_VALUE, 111);
        op[111] = getOpcodeRnm(67, 711); // nonwsNonPipe
        op[112] = getOpcodeRep((char)0, Character.MAX_VALUE, 113);
        {int[] a = {114,116}; op[113] = getOpcodeCat(a);}
        op[114] = getOpcodeRep((char)1, Character.MAX_VALUE, 115);
        op[115] = getOpcodeRnm(58, 702); // SP
        op[116] = getOpcodeRep((char)1, Character.MAX_VALUE, 117);
        op[117] = getOpcodeRnm(67, 711); // nonwsNonPipe
        {int[] a = {119,120}; op[118] = getOpcodeAlt(a);}
        {char[] a = {42}; op[119] = getOpcodeTls(a);}
        {int[] a = {121,124,127}; op[120] = getOpcodeCat(a);}
        {int[] a = {122,123}; op[121] = getOpcodeAlt(a);}
        {char[] a = {97}; op[122] = getOpcodeTls(a);}
        {char[] a = {65}; op[123] = getOpcodeTls(a);}
        {int[] a = {125,126}; op[124] = getOpcodeAlt(a);}
        {char[] a = {110}; op[125] = getOpcodeTls(a);}
        {char[] a = {78}; op[126] = getOpcodeTls(a);}
        {int[] a = {128,129}; op[127] = getOpcodeAlt(a);}
        {char[] a = {121}; op[128] = getOpcodeTls(a);}
        {char[] a = {89}; op[129] = getOpcodeTls(a);}
        {int[] a = {131,132,133,134,135,136}; op[130] = getOpcodeAlt(a);}
        op[131] = getOpcodeRnm(19, 235); // childOf
        op[132] = getOpcodeRnm(18, 177); // descendantOrSelfOf
        op[133] = getOpcodeRnm(17, 137); // descendantOf
        op[134] = getOpcodeRnm(22, 346); // parentOf
        op[135] = getOpcodeRnm(21, 294); // ancestorOrSelfOf
        op[136] = getOpcodeRnm(20, 260); // ancestorOf
        {int[] a = {138,139}; op[137] = getOpcodeAlt(a);}
        {char[] a = {60}; op[138] = getOpcodeTls(a);}
        {int[] a = {140,143,146,149,152,155,158,161,164,167,170,173,176}; op[139] = getOpcodeCat(a);}
        {int[] a = {141,142}; op[140] = getOpcodeAlt(a);}
        {char[] a = {100}; op[141] = getOpcodeTls(a);}
        {char[] a = {68}; op[142] = getOpcodeTls(a);}
        {int[] a = {144,145}; op[143] = getOpcodeAlt(a);}
        {char[] a = {101}; op[144] = getOpcodeTls(a);}
        {char[] a = {69}; op[145] = getOpcodeTls(a);}
        {int[] a = {147,148}; op[146] = getOpcodeAlt(a);}
        {char[] a = {115}; op[147] = getOpcodeTls(a);}
        {char[] a = {83}; op[148] = getOpcodeTls(a);}
        {int[] a = {150,151}; op[149] = getOpcodeAlt(a);}
        {char[] a = {99}; op[150] = getOpcodeTls(a);}
        {char[] a = {67}; op[151] = getOpcodeTls(a);}
        {int[] a = {153,154}; op[152] = getOpcodeAlt(a);}
        {char[] a = {101}; op[153] = getOpcodeTls(a);}
        {char[] a = {69}; op[154] = getOpcodeTls(a);}
        {int[] a = {156,157}; op[155] = getOpcodeAlt(a);}
        {char[] a = {110}; op[156] = getOpcodeTls(a);}
        {char[] a = {78}; op[157] = getOpcodeTls(a);}
        {int[] a = {159,160}; op[158] = getOpcodeAlt(a);}
        {char[] a = {100}; op[159] = getOpcodeTls(a);}
        {char[] a = {68}; op[160] = getOpcodeTls(a);}
        {int[] a = {162,163}; op[161] = getOpcodeAlt(a);}
        {char[] a = {97}; op[162] = getOpcodeTls(a);}
        {char[] a = {65}; op[163] = getOpcodeTls(a);}
        {int[] a = {165,166}; op[164] = getOpcodeAlt(a);}
        {char[] a = {110}; op[165] = getOpcodeTls(a);}
        {char[] a = {78}; op[166] = getOpcodeTls(a);}
        {int[] a = {168,169}; op[167] = getOpcodeAlt(a);}
        {char[] a = {116}; op[168] = getOpcodeTls(a);}
        {char[] a = {84}; op[169] = getOpcodeTls(a);}
        {int[] a = {171,172}; op[170] = getOpcodeAlt(a);}
        {char[] a = {111}; op[171] = getOpcodeTls(a);}
        {char[] a = {79}; op[172] = getOpcodeTls(a);}
        {int[] a = {174,175}; op[173] = getOpcodeAlt(a);}
        {char[] a = {102}; op[174] = getOpcodeTls(a);}
        {char[] a = {70}; op[175] = getOpcodeTls(a);}
        op[176] = getOpcodeRnm(53, 665); // mws
        {int[] a = {178,179}; op[177] = getOpcodeAlt(a);}
        {char[] a = {60,60}; op[178] = getOpcodeTls(a);}
        {int[] a = {180,183,186,189,192,195,198,201,204,207,210,213,216,219,222,225,228,231,234}; op[179] = getOpcodeCat(a);}
        {int[] a = {181,182}; op[180] = getOpcodeAlt(a);}
        {char[] a = {100}; op[181] = getOpcodeTls(a);}
        {char[] a = {68}; op[182] = getOpcodeTls(a);}
        {int[] a = {184,185}; op[183] = getOpcodeAlt(a);}
        {char[] a = {101}; op[184] = getOpcodeTls(a);}
        {char[] a = {69}; op[185] = getOpcodeTls(a);}
        {int[] a = {187,188}; op[186] = getOpcodeAlt(a);}
        {char[] a = {115}; op[187] = getOpcodeTls(a);}
        {char[] a = {83}; op[188] = getOpcodeTls(a);}
        {int[] a = {190,191}; op[189] = getOpcodeAlt(a);}
        {char[] a = {99}; op[190] = getOpcodeTls(a);}
        {char[] a = {67}; op[191] = getOpcodeTls(a);}
        {int[] a = {193,194}; op[192] = getOpcodeAlt(a);}
        {char[] a = {101}; op[193] = getOpcodeTls(a);}
        {char[] a = {69}; op[194] = getOpcodeTls(a);}
        {int[] a = {196,197}; op[195] = getOpcodeAlt(a);}
        {char[] a = {110}; op[196] = getOpcodeTls(a);}
        {char[] a = {78}; op[197] = getOpcodeTls(a);}
        {int[] a = {199,200}; op[198] = getOpcodeAlt(a);}
        {char[] a = {100}; op[199] = getOpcodeTls(a);}
        {char[] a = {68}; op[200] = getOpcodeTls(a);}
        {int[] a = {202,203}; op[201] = getOpcodeAlt(a);}
        {char[] a = {97}; op[202] = getOpcodeTls(a);}
        {char[] a = {65}; op[203] = getOpcodeTls(a);}
        {int[] a = {205,206}; op[204] = getOpcodeAlt(a);}
        {char[] a = {110}; op[205] = getOpcodeTls(a);}
        {char[] a = {78}; op[206] = getOpcodeTls(a);}
        {int[] a = {208,209}; op[207] = getOpcodeAlt(a);}
        {char[] a = {116}; op[208] = getOpcodeTls(a);}
        {char[] a = {84}; op[209] = getOpcodeTls(a);}
        {int[] a = {211,212}; op[210] = getOpcodeAlt(a);}
        {char[] a = {111}; op[211] = getOpcodeTls(a);}
        {char[] a = {79}; op[212] = getOpcodeTls(a);}
        {int[] a = {214,215}; op[213] = getOpcodeAlt(a);}
        {char[] a = {114}; op[214] = getOpcodeTls(a);}
        {char[] a = {82}; op[215] = getOpcodeTls(a);}
        {int[] a = {217,218}; op[216] = getOpcodeAlt(a);}
        {char[] a = {115}; op[217] = getOpcodeTls(a);}
        {char[] a = {83}; op[218] = getOpcodeTls(a);}
        {int[] a = {220,221}; op[219] = getOpcodeAlt(a);}
        {char[] a = {101}; op[220] = getOpcodeTls(a);}
        {char[] a = {69}; op[221] = getOpcodeTls(a);}
        {int[] a = {223,224}; op[222] = getOpcodeAlt(a);}
        {char[] a = {108}; op[223] = getOpcodeTls(a);}
        {char[] a = {76}; op[224] = getOpcodeTls(a);}
        {int[] a = {226,227}; op[225] = getOpcodeAlt(a);}
        {char[] a = {102}; op[226] = getOpcodeTls(a);}
        {char[] a = {70}; op[227] = getOpcodeTls(a);}
        {int[] a = {229,230}; op[228] = getOpcodeAlt(a);}
        {char[] a = {111}; op[229] = getOpcodeTls(a);}
        {char[] a = {79}; op[230] = getOpcodeTls(a);}
        {int[] a = {232,233}; op[231] = getOpcodeAlt(a);}
        {char[] a = {102}; op[232] = getOpcodeTls(a);}
        {char[] a = {70}; op[233] = getOpcodeTls(a);}
        op[234] = getOpcodeRnm(53, 665); // mws
        {int[] a = {236,237}; op[235] = getOpcodeAlt(a);}
        {char[] a = {60,33}; op[236] = getOpcodeTls(a);}
        {int[] a = {238,241,244,247,250,253,256,259}; op[237] = getOpcodeCat(a);}
        {int[] a = {239,240}; op[238] = getOpcodeAlt(a);}
        {char[] a = {99}; op[239] = getOpcodeTls(a);}
        {char[] a = {67}; op[240] = getOpcodeTls(a);}
        {int[] a = {242,243}; op[241] = getOpcodeAlt(a);}
        {char[] a = {104}; op[242] = getOpcodeTls(a);}
        {char[] a = {72}; op[243] = getOpcodeTls(a);}
        {int[] a = {245,246}; op[244] = getOpcodeAlt(a);}
        {char[] a = {105}; op[245] = getOpcodeTls(a);}
        {char[] a = {73}; op[246] = getOpcodeTls(a);}
        {int[] a = {248,249}; op[247] = getOpcodeAlt(a);}
        {char[] a = {108}; op[248] = getOpcodeTls(a);}
        {char[] a = {76}; op[249] = getOpcodeTls(a);}
        {int[] a = {251,252}; op[250] = getOpcodeAlt(a);}
        {char[] a = {100}; op[251] = getOpcodeTls(a);}
        {char[] a = {68}; op[252] = getOpcodeTls(a);}
        {int[] a = {254,255}; op[253] = getOpcodeAlt(a);}
        {char[] a = {111}; op[254] = getOpcodeTls(a);}
        {char[] a = {79}; op[255] = getOpcodeTls(a);}
        {int[] a = {257,258}; op[256] = getOpcodeAlt(a);}
        {char[] a = {102}; op[257] = getOpcodeTls(a);}
        {char[] a = {70}; op[258] = getOpcodeTls(a);}
        op[259] = getOpcodeRnm(53, 665); // mws
        {int[] a = {261,262}; op[260] = getOpcodeAlt(a);}
        {char[] a = {62}; op[261] = getOpcodeTls(a);}
        {int[] a = {263,266,269,272,275,278,281,284,287,290,293}; op[262] = getOpcodeCat(a);}
        {int[] a = {264,265}; op[263] = getOpcodeAlt(a);}
        {char[] a = {97}; op[264] = getOpcodeTls(a);}
        {char[] a = {65}; op[265] = getOpcodeTls(a);}
        {int[] a = {267,268}; op[266] = getOpcodeAlt(a);}
        {char[] a = {110}; op[267] = getOpcodeTls(a);}
        {char[] a = {78}; op[268] = getOpcodeTls(a);}
        {int[] a = {270,271}; op[269] = getOpcodeAlt(a);}
        {char[] a = {99}; op[270] = getOpcodeTls(a);}
        {char[] a = {67}; op[271] = getOpcodeTls(a);}
        {int[] a = {273,274}; op[272] = getOpcodeAlt(a);}
        {char[] a = {101}; op[273] = getOpcodeTls(a);}
        {char[] a = {69}; op[274] = getOpcodeTls(a);}
        {int[] a = {276,277}; op[275] = getOpcodeAlt(a);}
        {char[] a = {115}; op[276] = getOpcodeTls(a);}
        {char[] a = {83}; op[277] = getOpcodeTls(a);}
        {int[] a = {279,280}; op[278] = getOpcodeAlt(a);}
        {char[] a = {116}; op[279] = getOpcodeTls(a);}
        {char[] a = {84}; op[280] = getOpcodeTls(a);}
        {int[] a = {282,283}; op[281] = getOpcodeAlt(a);}
        {char[] a = {111}; op[282] = getOpcodeTls(a);}
        {char[] a = {79}; op[283] = getOpcodeTls(a);}
        {int[] a = {285,286}; op[284] = getOpcodeAlt(a);}
        {char[] a = {114}; op[285] = getOpcodeTls(a);}
        {char[] a = {82}; op[286] = getOpcodeTls(a);}
        {int[] a = {288,289}; op[287] = getOpcodeAlt(a);}
        {char[] a = {111}; op[288] = getOpcodeTls(a);}
        {char[] a = {79}; op[289] = getOpcodeTls(a);}
        {int[] a = {291,292}; op[290] = getOpcodeAlt(a);}
        {char[] a = {102}; op[291] = getOpcodeTls(a);}
        {char[] a = {70}; op[292] = getOpcodeTls(a);}
        op[293] = getOpcodeRnm(53, 665); // mws
        {int[] a = {295,296}; op[294] = getOpcodeAlt(a);}
        {char[] a = {62,62}; op[295] = getOpcodeTls(a);}
        {int[] a = {297,300,303,306,309,312,315,318,321,324,327,330,333,336,339,342,345}; op[296] = getOpcodeCat(a);}
        {int[] a = {298,299}; op[297] = getOpcodeAlt(a);}
        {char[] a = {97}; op[298] = getOpcodeTls(a);}
        {char[] a = {65}; op[299] = getOpcodeTls(a);}
        {int[] a = {301,302}; op[300] = getOpcodeAlt(a);}
        {char[] a = {110}; op[301] = getOpcodeTls(a);}
        {char[] a = {78}; op[302] = getOpcodeTls(a);}
        {int[] a = {304,305}; op[303] = getOpcodeAlt(a);}
        {char[] a = {99}; op[304] = getOpcodeTls(a);}
        {char[] a = {67}; op[305] = getOpcodeTls(a);}
        {int[] a = {307,308}; op[306] = getOpcodeAlt(a);}
        {char[] a = {101}; op[307] = getOpcodeTls(a);}
        {char[] a = {69}; op[308] = getOpcodeTls(a);}
        {int[] a = {310,311}; op[309] = getOpcodeAlt(a);}
        {char[] a = {115}; op[310] = getOpcodeTls(a);}
        {char[] a = {83}; op[311] = getOpcodeTls(a);}
        {int[] a = {313,314}; op[312] = getOpcodeAlt(a);}
        {char[] a = {116}; op[313] = getOpcodeTls(a);}
        {char[] a = {84}; op[314] = getOpcodeTls(a);}
        {int[] a = {316,317}; op[315] = getOpcodeAlt(a);}
        {char[] a = {111}; op[316] = getOpcodeTls(a);}
        {char[] a = {79}; op[317] = getOpcodeTls(a);}
        {int[] a = {319,320}; op[318] = getOpcodeAlt(a);}
        {char[] a = {114}; op[319] = getOpcodeTls(a);}
        {char[] a = {82}; op[320] = getOpcodeTls(a);}
        {int[] a = {322,323}; op[321] = getOpcodeAlt(a);}
        {char[] a = {111}; op[322] = getOpcodeTls(a);}
        {char[] a = {79}; op[323] = getOpcodeTls(a);}
        {int[] a = {325,326}; op[324] = getOpcodeAlt(a);}
        {char[] a = {114}; op[325] = getOpcodeTls(a);}
        {char[] a = {82}; op[326] = getOpcodeTls(a);}
        {int[] a = {328,329}; op[327] = getOpcodeAlt(a);}
        {char[] a = {115}; op[328] = getOpcodeTls(a);}
        {char[] a = {83}; op[329] = getOpcodeTls(a);}
        {int[] a = {331,332}; op[330] = getOpcodeAlt(a);}
        {char[] a = {101}; op[331] = getOpcodeTls(a);}
        {char[] a = {69}; op[332] = getOpcodeTls(a);}
        {int[] a = {334,335}; op[333] = getOpcodeAlt(a);}
        {char[] a = {108}; op[334] = getOpcodeTls(a);}
        {char[] a = {76}; op[335] = getOpcodeTls(a);}
        {int[] a = {337,338}; op[336] = getOpcodeAlt(a);}
        {char[] a = {102}; op[337] = getOpcodeTls(a);}
        {char[] a = {70}; op[338] = getOpcodeTls(a);}
        {int[] a = {340,341}; op[339] = getOpcodeAlt(a);}
        {char[] a = {111}; op[340] = getOpcodeTls(a);}
        {char[] a = {79}; op[341] = getOpcodeTls(a);}
        {int[] a = {343,344}; op[342] = getOpcodeAlt(a);}
        {char[] a = {102}; op[343] = getOpcodeTls(a);}
        {char[] a = {70}; op[344] = getOpcodeTls(a);}
        op[345] = getOpcodeRnm(53, 665); // mws
        {int[] a = {347,348}; op[346] = getOpcodeAlt(a);}
        {char[] a = {62,33}; op[347] = getOpcodeTls(a);}
        {int[] a = {349,352,355,358,361,364,367,370,373}; op[348] = getOpcodeCat(a);}
        {int[] a = {350,351}; op[349] = getOpcodeAlt(a);}
        {char[] a = {112}; op[350] = getOpcodeTls(a);}
        {char[] a = {80}; op[351] = getOpcodeTls(a);}
        {int[] a = {353,354}; op[352] = getOpcodeAlt(a);}
        {char[] a = {97}; op[353] = getOpcodeTls(a);}
        {char[] a = {65}; op[354] = getOpcodeTls(a);}
        {int[] a = {356,357}; op[355] = getOpcodeAlt(a);}
        {char[] a = {114}; op[356] = getOpcodeTls(a);}
        {char[] a = {82}; op[357] = getOpcodeTls(a);}
        {int[] a = {359,360}; op[358] = getOpcodeAlt(a);}
        {char[] a = {101}; op[359] = getOpcodeTls(a);}
        {char[] a = {69}; op[360] = getOpcodeTls(a);}
        {int[] a = {362,363}; op[361] = getOpcodeAlt(a);}
        {char[] a = {110}; op[362] = getOpcodeTls(a);}
        {char[] a = {78}; op[363] = getOpcodeTls(a);}
        {int[] a = {365,366}; op[364] = getOpcodeAlt(a);}
        {char[] a = {116}; op[365] = getOpcodeTls(a);}
        {char[] a = {84}; op[366] = getOpcodeTls(a);}
        {int[] a = {368,369}; op[367] = getOpcodeAlt(a);}
        {char[] a = {111}; op[368] = getOpcodeTls(a);}
        {char[] a = {79}; op[369] = getOpcodeTls(a);}
        {int[] a = {371,372}; op[370] = getOpcodeAlt(a);}
        {char[] a = {102}; op[371] = getOpcodeTls(a);}
        {char[] a = {70}; op[372] = getOpcodeTls(a);}
        op[373] = getOpcodeRnm(53, 665); // mws
        {int[] a = {375,386}; op[374] = getOpcodeAlt(a);}
        {int[] a = {376,379,382,385}; op[375] = getOpcodeCat(a);}
        {int[] a = {377,378}; op[376] = getOpcodeAlt(a);}
        {char[] a = {97}; op[377] = getOpcodeTls(a);}
        {char[] a = {65}; op[378] = getOpcodeTls(a);}
        {int[] a = {380,381}; op[379] = getOpcodeAlt(a);}
        {char[] a = {110}; op[380] = getOpcodeTls(a);}
        {char[] a = {78}; op[381] = getOpcodeTls(a);}
        {int[] a = {383,384}; op[382] = getOpcodeAlt(a);}
        {char[] a = {100}; op[383] = getOpcodeTls(a);}
        {char[] a = {68}; op[384] = getOpcodeTls(a);}
        op[385] = getOpcodeRnm(53, 665); // mws
        {char[] a = {44}; op[386] = getOpcodeTls(a);}
        {int[] a = {388,391,394}; op[387] = getOpcodeCat(a);}
        {int[] a = {389,390}; op[388] = getOpcodeAlt(a);}
        {char[] a = {111}; op[389] = getOpcodeTls(a);}
        {char[] a = {79}; op[390] = getOpcodeTls(a);}
        {int[] a = {392,393}; op[391] = getOpcodeAlt(a);}
        {char[] a = {114}; op[392] = getOpcodeTls(a);}
        {char[] a = {82}; op[393] = getOpcodeTls(a);}
        op[394] = getOpcodeRnm(53, 665); // mws
        {int[] a = {396,399,402,405,408,411}; op[395] = getOpcodeCat(a);}
        {int[] a = {397,398}; op[396] = getOpcodeAlt(a);}
        {char[] a = {109}; op[397] = getOpcodeTls(a);}
        {char[] a = {77}; op[398] = getOpcodeTls(a);}
        {int[] a = {400,401}; op[399] = getOpcodeAlt(a);}
        {char[] a = {105}; op[400] = getOpcodeTls(a);}
        {char[] a = {73}; op[401] = getOpcodeTls(a);}
        {int[] a = {403,404}; op[402] = getOpcodeAlt(a);}
        {char[] a = {110}; op[403] = getOpcodeTls(a);}
        {char[] a = {78}; op[404] = getOpcodeTls(a);}
        {int[] a = {406,407}; op[405] = getOpcodeAlt(a);}
        {char[] a = {117}; op[406] = getOpcodeTls(a);}
        {char[] a = {85}; op[407] = getOpcodeTls(a);}
        {int[] a = {409,410}; op[408] = getOpcodeAlt(a);}
        {char[] a = {115}; op[409] = getOpcodeTls(a);}
        {char[] a = {83}; op[410] = getOpcodeTls(a);}
        op[411] = getOpcodeRnm(53, 665); // mws
        {int[] a = {413,414,415}; op[412] = getOpcodeCat(a);}
        op[413] = getOpcodeRnm(29, 431); // subRefinement
        op[414] = getOpcodeRnm(52, 658); // ws
        op[415] = getOpcodeRep((char)0, (char)1, 416);
        {int[] a = {417,418}; op[416] = getOpcodeAlt(a);}
        op[417] = getOpcodeRnm(27, 419); // conjunctionRefinementSet
        op[418] = getOpcodeRnm(28, 425); // disjunctionRefinementSet
        op[419] = getOpcodeRep((char)1, Character.MAX_VALUE, 420);
        {int[] a = {421,422,423,424}; op[420] = getOpcodeCat(a);}
        op[421] = getOpcodeRnm(52, 658); // ws
        op[422] = getOpcodeRnm(23, 374); // conjunction
        op[423] = getOpcodeRnm(52, 658); // ws
        op[424] = getOpcodeRnm(29, 431); // subRefinement
        op[425] = getOpcodeRep((char)1, Character.MAX_VALUE, 426);
        {int[] a = {427,428,429,430}; op[426] = getOpcodeCat(a);}
        op[427] = getOpcodeRnm(52, 658); // ws
        op[428] = getOpcodeRnm(24, 387); // disjunction
        op[429] = getOpcodeRnm(52, 658); // ws
        op[430] = getOpcodeRnm(29, 431); // subRefinement
        {int[] a = {432,433,434}; op[431] = getOpcodeAlt(a);}
        op[432] = getOpcodeRnm(30, 440); // eclAttributeSet
        op[433] = getOpcodeRnm(34, 467); // eclAttributeGroup
        {int[] a = {435,436,437,438,439}; op[434] = getOpcodeCat(a);}
        {char[] a = {40}; op[435] = getOpcodeTls(a);}
        op[436] = getOpcodeRnm(52, 658); // ws
        op[437] = getOpcodeRnm(26, 412); // eclRefinement
        op[438] = getOpcodeRnm(52, 658); // ws
        {char[] a = {41}; op[439] = getOpcodeTls(a);}
        {int[] a = {441,442,443}; op[440] = getOpcodeCat(a);}
        op[441] = getOpcodeRnm(33, 459); // subAttributeSet
        op[442] = getOpcodeRnm(52, 658); // ws
        op[443] = getOpcodeRep((char)0, (char)1, 444);
        {int[] a = {445,446}; op[444] = getOpcodeAlt(a);}
        op[445] = getOpcodeRnm(31, 447); // conjunctionAttributeSet
        op[446] = getOpcodeRnm(32, 453); // disjunctionAttributeSet
        op[447] = getOpcodeRep((char)1, Character.MAX_VALUE, 448);
        {int[] a = {449,450,451,452}; op[448] = getOpcodeCat(a);}
        op[449] = getOpcodeRnm(52, 658); // ws
        op[450] = getOpcodeRnm(23, 374); // conjunction
        op[451] = getOpcodeRnm(52, 658); // ws
        op[452] = getOpcodeRnm(33, 459); // subAttributeSet
        op[453] = getOpcodeRep((char)1, Character.MAX_VALUE, 454);
        {int[] a = {455,456,457,458}; op[454] = getOpcodeCat(a);}
        op[455] = getOpcodeRnm(52, 658); // ws
        op[456] = getOpcodeRnm(24, 387); // disjunction
        op[457] = getOpcodeRnm(52, 658); // ws
        op[458] = getOpcodeRnm(33, 459); // subAttributeSet
        {int[] a = {460,461}; op[459] = getOpcodeAlt(a);}
        op[460] = getOpcodeRnm(35, 479); // eclAttribute
        {int[] a = {462,463,464,465,466}; op[461] = getOpcodeCat(a);}
        {char[] a = {40}; op[462] = getOpcodeTls(a);}
        op[463] = getOpcodeRnm(52, 658); // ws
        op[464] = getOpcodeRnm(30, 440); // eclAttributeSet
        op[465] = getOpcodeRnm(52, 658); // ws
        {char[] a = {41}; op[466] = getOpcodeTls(a);}
        {int[] a = {468,474,475,476,477,478}; op[467] = getOpcodeCat(a);}
        op[468] = getOpcodeRep((char)0, (char)1, 469);
        {int[] a = {470,471,472,473}; op[469] = getOpcodeCat(a);}
        {char[] a = {91}; op[470] = getOpcodeTls(a);}
        op[471] = getOpcodeRnm(36, 508); // cardinality
        {char[] a = {93}; op[472] = getOpcodeTls(a);}
        op[473] = getOpcodeRnm(52, 658); // ws
        {char[] a = {123}; op[474] = getOpcodeTls(a);}
        op[475] = getOpcodeRnm(52, 658); // ws
        op[476] = getOpcodeRnm(30, 440); // eclAttributeSet
        op[477] = getOpcodeRnm(52, 658); // ws
        {char[] a = {125}; op[478] = getOpcodeTls(a);}
        {int[] a = {480,486,490,491,492}; op[479] = getOpcodeCat(a);}
        op[480] = getOpcodeRep((char)0, (char)1, 481);
        {int[] a = {482,483,484,485}; op[481] = getOpcodeCat(a);}
        {char[] a = {91}; op[482] = getOpcodeTls(a);}
        op[483] = getOpcodeRnm(36, 508); // cardinality
        {char[] a = {93}; op[484] = getOpcodeTls(a);}
        op[485] = getOpcodeRnm(52, 658); // ws
        op[486] = getOpcodeRep((char)0, (char)1, 487);
        {int[] a = {488,489}; op[487] = getOpcodeCat(a);}
        op[488] = getOpcodeRnm(41, 542); // reverseFlag
        op[489] = getOpcodeRnm(52, 658); // ws
        op[490] = getOpcodeRnm(42, 572); // eclAttributeName
        op[491] = getOpcodeRnm(52, 658); // ws
        {int[] a = {493,497,502}; op[492] = getOpcodeAlt(a);}
        {int[] a = {494,495,496}; op[493] = getOpcodeCat(a);}
        op[494] = getOpcodeRnm(43, 573); // expressionComparisonOperator
        op[495] = getOpcodeRnm(52, 658); // ws
        op[496] = getOpcodeRnm(8, 50); // subExpressionConstraint
        {int[] a = {498,499,500,501}; op[497] = getOpcodeCat(a);}
        op[498] = getOpcodeRnm(44, 589); // numericComparisonOperator
        op[499] = getOpcodeRnm(52, 658); // ws
        {char[] a = {35}; op[500] = getOpcodeTls(a);}
        op[501] = getOpcodeRnm(46, 625); // numericValue
        {int[] a = {503,504,505,506,507}; op[502] = getOpcodeCat(a);}
        op[503] = getOpcodeRnm(45, 609); // stringComparisonOperator
        op[504] = getOpcodeRnm(52, 658); // ws
        op[505] = getOpcodeRnm(62, 706); // QM
        op[506] = getOpcodeRnm(47, 633); // stringValue
        op[507] = getOpcodeRnm(62, 706); // QM
        {int[] a = {509,510,511}; op[508] = getOpcodeCat(a);}
        op[509] = getOpcodeRnm(37, 512); // minValue
        op[510] = getOpcodeRnm(38, 513); // to
        op[511] = getOpcodeRnm(39, 524); // maxValue
        op[512] = getOpcodeRnm(50, 648); // nonNegativeIntegerValue
        {int[] a = {514,515}; op[513] = getOpcodeAlt(a);}
        {char[] a = {46,46}; op[514] = getOpcodeTls(a);}
        {int[] a = {516,517,520,523}; op[515] = getOpcodeCat(a);}
        op[516] = getOpcodeRnm(53, 665); // mws
        {int[] a = {518,519}; op[517] = getOpcodeAlt(a);}
        {char[] a = {116}; op[518] = getOpcodeTls(a);}
        {char[] a = {84}; op[519] = getOpcodeTls(a);}
        {int[] a = {521,522}; op[520] = getOpcodeAlt(a);}
        {char[] a = {111}; op[521] = getOpcodeTls(a);}
        {char[] a = {79}; op[522] = getOpcodeTls(a);}
        op[523] = getOpcodeRnm(53, 665); // mws
        {int[] a = {525,526}; op[524] = getOpcodeAlt(a);}
        op[525] = getOpcodeRnm(50, 648); // nonNegativeIntegerValue
        op[526] = getOpcodeRnm(40, 527); // many
        {int[] a = {528,529}; op[527] = getOpcodeAlt(a);}
        {char[] a = {42}; op[528] = getOpcodeTls(a);}
        {int[] a = {530,533,536,539}; op[529] = getOpcodeCat(a);}
        {int[] a = {531,532}; op[530] = getOpcodeAlt(a);}
        {char[] a = {109}; op[531] = getOpcodeTls(a);}
        {char[] a = {77}; op[532] = getOpcodeTls(a);}
        {int[] a = {534,535}; op[533] = getOpcodeAlt(a);}
        {char[] a = {97}; op[534] = getOpcodeTls(a);}
        {char[] a = {65}; op[535] = getOpcodeTls(a);}
        {int[] a = {537,538}; op[536] = getOpcodeAlt(a);}
        {char[] a = {110}; op[537] = getOpcodeTls(a);}
        {char[] a = {78}; op[538] = getOpcodeTls(a);}
        {int[] a = {540,541}; op[539] = getOpcodeAlt(a);}
        {char[] a = {121}; op[540] = getOpcodeTls(a);}
        {char[] a = {89}; op[541] = getOpcodeTls(a);}
        {int[] a = {543,571}; op[542] = getOpcodeAlt(a);}
        {int[] a = {544,547,550,553,556,559,562,565,568}; op[543] = getOpcodeCat(a);}
        {int[] a = {545,546}; op[544] = getOpcodeAlt(a);}
        {char[] a = {114}; op[545] = getOpcodeTls(a);}
        {char[] a = {82}; op[546] = getOpcodeTls(a);}
        {int[] a = {548,549}; op[547] = getOpcodeAlt(a);}
        {char[] a = {101}; op[548] = getOpcodeTls(a);}
        {char[] a = {69}; op[549] = getOpcodeTls(a);}
        {int[] a = {551,552}; op[550] = getOpcodeAlt(a);}
        {char[] a = {118}; op[551] = getOpcodeTls(a);}
        {char[] a = {86}; op[552] = getOpcodeTls(a);}
        {int[] a = {554,555}; op[553] = getOpcodeAlt(a);}
        {char[] a = {101}; op[554] = getOpcodeTls(a);}
        {char[] a = {69}; op[555] = getOpcodeTls(a);}
        {int[] a = {557,558}; op[556] = getOpcodeAlt(a);}
        {char[] a = {114}; op[557] = getOpcodeTls(a);}
        {char[] a = {82}; op[558] = getOpcodeTls(a);}
        {int[] a = {560,561}; op[559] = getOpcodeAlt(a);}
        {char[] a = {115}; op[560] = getOpcodeTls(a);}
        {char[] a = {83}; op[561] = getOpcodeTls(a);}
        {int[] a = {563,564}; op[562] = getOpcodeAlt(a);}
        {char[] a = {101}; op[563] = getOpcodeTls(a);}
        {char[] a = {69}; op[564] = getOpcodeTls(a);}
        {int[] a = {566,567}; op[565] = getOpcodeAlt(a);}
        {char[] a = {111}; op[566] = getOpcodeTls(a);}
        {char[] a = {79}; op[567] = getOpcodeTls(a);}
        {int[] a = {569,570}; op[568] = getOpcodeAlt(a);}
        {char[] a = {102}; op[569] = getOpcodeTls(a);}
        {char[] a = {70}; op[570] = getOpcodeTls(a);}
        {char[] a = {82}; op[571] = getOpcodeTls(a);}
        op[572] = getOpcodeRnm(8, 50); // subExpressionConstraint
        {int[] a = {574,575,576,588}; op[573] = getOpcodeAlt(a);}
        {char[] a = {61}; op[574] = getOpcodeTls(a);}
        {char[] a = {33,61}; op[575] = getOpcodeTls(a);}
        {int[] a = {577,580,583,586,587}; op[576] = getOpcodeCat(a);}
        {int[] a = {578,579}; op[577] = getOpcodeAlt(a);}
        {char[] a = {110}; op[578] = getOpcodeTls(a);}
        {char[] a = {78}; op[579] = getOpcodeTls(a);}
        {int[] a = {581,582}; op[580] = getOpcodeAlt(a);}
        {char[] a = {111}; op[581] = getOpcodeTls(a);}
        {char[] a = {79}; op[582] = getOpcodeTls(a);}
        {int[] a = {584,585}; op[583] = getOpcodeAlt(a);}
        {char[] a = {116}; op[584] = getOpcodeTls(a);}
        {char[] a = {84}; op[585] = getOpcodeTls(a);}
        op[586] = getOpcodeRnm(52, 658); // ws
        {char[] a = {61}; op[587] = getOpcodeTls(a);}
        {char[] a = {60,62}; op[588] = getOpcodeTls(a);}
        {int[] a = {590,591,592,604,605,606,607,608}; op[589] = getOpcodeAlt(a);}
        {char[] a = {61}; op[590] = getOpcodeTls(a);}
        {char[] a = {33,61}; op[591] = getOpcodeTls(a);}
        {int[] a = {593,596,599,602,603}; op[592] = getOpcodeCat(a);}
        {int[] a = {594,595}; op[593] = getOpcodeAlt(a);}
        {char[] a = {110}; op[594] = getOpcodeTls(a);}
        {char[] a = {78}; op[595] = getOpcodeTls(a);}
        {int[] a = {597,598}; op[596] = getOpcodeAlt(a);}
        {char[] a = {111}; op[597] = getOpcodeTls(a);}
        {char[] a = {79}; op[598] = getOpcodeTls(a);}
        {int[] a = {600,601}; op[599] = getOpcodeAlt(a);}
        {char[] a = {116}; op[600] = getOpcodeTls(a);}
        {char[] a = {84}; op[601] = getOpcodeTls(a);}
        op[602] = getOpcodeRnm(52, 658); // ws
        {char[] a = {61}; op[603] = getOpcodeTls(a);}
        {char[] a = {60,62}; op[604] = getOpcodeTls(a);}
        {char[] a = {60,61}; op[605] = getOpcodeTls(a);}
        {char[] a = {60}; op[606] = getOpcodeTls(a);}
        {char[] a = {62,61}; op[607] = getOpcodeTls(a);}
        {char[] a = {62}; op[608] = getOpcodeTls(a);}
        {int[] a = {610,611,612,624}; op[609] = getOpcodeAlt(a);}
        {char[] a = {61}; op[610] = getOpcodeTls(a);}
        {char[] a = {33,61}; op[611] = getOpcodeTls(a);}
        {int[] a = {613,616,619,622,623}; op[612] = getOpcodeCat(a);}
        {int[] a = {614,615}; op[613] = getOpcodeAlt(a);}
        {char[] a = {110}; op[614] = getOpcodeTls(a);}
        {char[] a = {78}; op[615] = getOpcodeTls(a);}
        {int[] a = {617,618}; op[616] = getOpcodeAlt(a);}
        {char[] a = {111}; op[617] = getOpcodeTls(a);}
        {char[] a = {79}; op[618] = getOpcodeTls(a);}
        {int[] a = {620,621}; op[619] = getOpcodeAlt(a);}
        {char[] a = {116}; op[620] = getOpcodeTls(a);}
        {char[] a = {84}; op[621] = getOpcodeTls(a);}
        op[622] = getOpcodeRnm(52, 658); // ws
        {char[] a = {61}; op[623] = getOpcodeTls(a);}
        {char[] a = {60,62}; op[624] = getOpcodeTls(a);}
        {int[] a = {626,630}; op[625] = getOpcodeCat(a);}
        op[626] = getOpcodeRep((char)0, (char)1, 627);
        {int[] a = {628,629}; op[627] = getOpcodeAlt(a);}
        {char[] a = {45}; op[628] = getOpcodeTls(a);}
        {char[] a = {43}; op[629] = getOpcodeTls(a);}
        {int[] a = {631,632}; op[630] = getOpcodeAlt(a);}
        op[631] = getOpcodeRnm(49, 643); // decimalValue
        op[632] = getOpcodeRnm(48, 637); // integerValue
        op[633] = getOpcodeRep((char)1, Character.MAX_VALUE, 634);
        {int[] a = {635,636}; op[634] = getOpcodeAlt(a);}
        op[635] = getOpcodeRnm(68, 717); // anyNonEscapedChar
        op[636] = getOpcodeRnm(69, 728); // escapedChar
        {int[] a = {638,642}; op[637] = getOpcodeAlt(a);}
        {int[] a = {639,640}; op[638] = getOpcodeCat(a);}
        op[639] = getOpcodeRnm(66, 710); // digitNonZero
        op[640] = getOpcodeRep((char)0, Character.MAX_VALUE, 641);
        op[641] = getOpcodeRnm(64, 708); // digit
        op[642] = getOpcodeRnm(65, 709); // zero
        {int[] a = {644,645,646}; op[643] = getOpcodeCat(a);}
        op[644] = getOpcodeRnm(48, 637); // integerValue
        {char[] a = {46}; op[645] = getOpcodeTls(a);}
        op[646] = getOpcodeRep((char)1, Character.MAX_VALUE, 647);
        op[647] = getOpcodeRnm(64, 708); // digit
        {int[] a = {649,653}; op[648] = getOpcodeAlt(a);}
        {int[] a = {650,651}; op[649] = getOpcodeCat(a);}
        op[650] = getOpcodeRnm(66, 710); // digitNonZero
        op[651] = getOpcodeRep((char)0, Character.MAX_VALUE, 652);
        op[652] = getOpcodeRnm(64, 708); // digit
        op[653] = getOpcodeRnm(65, 709); // zero
        {int[] a = {655,656}; op[654] = getOpcodeCat(a);}
        op[655] = getOpcodeRnm(66, 710); // digitNonZero
        op[656] = getOpcodeRep((char)5, (char)17, 657);
        op[657] = getOpcodeRnm(64, 708); // digit
        op[658] = getOpcodeRep((char)0, Character.MAX_VALUE, 659);
        {int[] a = {660,661,662,663,664}; op[659] = getOpcodeAlt(a);}
        op[660] = getOpcodeRnm(58, 702); // SP
        op[661] = getOpcodeRnm(59, 703); // HTAB
        op[662] = getOpcodeRnm(60, 704); // CR
        op[663] = getOpcodeRnm(61, 705); // LF
        op[664] = getOpcodeRnm(54, 672); // comment
        op[665] = getOpcodeRep((char)1, Character.MAX_VALUE, 666);
        {int[] a = {667,668,669,670,671}; op[666] = getOpcodeAlt(a);}
        op[667] = getOpcodeRnm(58, 702); // SP
        op[668] = getOpcodeRnm(59, 703); // HTAB
        op[669] = getOpcodeRnm(60, 704); // CR
        op[670] = getOpcodeRnm(61, 705); // LF
        op[671] = getOpcodeRnm(54, 672); // comment
        {int[] a = {673,674,678}; op[672] = getOpcodeCat(a);}
        {char[] a = {47,42}; op[673] = getOpcodeTls(a);}
        op[674] = getOpcodeRep((char)0, Character.MAX_VALUE, 675);
        {int[] a = {676,677}; op[675] = getOpcodeAlt(a);}
        op[676] = getOpcodeRnm(55, 679); // nonStarChar
        op[677] = getOpcodeRnm(56, 689); // starWithNonFSlash
        {char[] a = {42,47}; op[678] = getOpcodeTls(a);}
        {int[] a = {680,681,682,683,684,685,686,687,688}; op[679] = getOpcodeAlt(a);}
        op[680] = getOpcodeRnm(58, 702); // SP
        op[681] = getOpcodeRnm(59, 703); // HTAB
        op[682] = getOpcodeRnm(60, 704); // CR
        op[683] = getOpcodeRnm(61, 705); // LF
        op[684] = getOpcodeTrg((char)33, (char)41);
        op[685] = getOpcodeTrg((char)43, (char)126);
        op[686] = getOpcodeRnm(70, 735); // UTF8-2
        op[687] = getOpcodeRnm(71, 738); // UTF8-3
        op[688] = getOpcodeRnm(72, 755); // UTF8-4
        {int[] a = {690,691}; op[689] = getOpcodeCat(a);}
        {char[] a = {42}; op[690] = getOpcodeTbs(a);}
        op[691] = getOpcodeRnm(57, 692); // nonFSlash
        {int[] a = {693,694,695,696,697,698,699,700,701}; op[692] = getOpcodeAlt(a);}
        op[693] = getOpcodeRnm(58, 702); // SP
        op[694] = getOpcodeRnm(59, 703); // HTAB
        op[695] = getOpcodeRnm(60, 704); // CR
        op[696] = getOpcodeRnm(61, 705); // LF
        op[697] = getOpcodeTrg((char)33, (char)46);
        op[698] = getOpcodeTrg((char)48, (char)126);
        op[699] = getOpcodeRnm(70, 735); // UTF8-2
        op[700] = getOpcodeRnm(71, 738); // UTF8-3
        op[701] = getOpcodeRnm(72, 755); // UTF8-4
        {char[] a = {32}; op[702] = getOpcodeTbs(a);}
        {char[] a = {9}; op[703] = getOpcodeTbs(a);}
        {char[] a = {13}; op[704] = getOpcodeTbs(a);}
        {char[] a = {10}; op[705] = getOpcodeTbs(a);}
        {char[] a = {34}; op[706] = getOpcodeTbs(a);}
        {char[] a = {92}; op[707] = getOpcodeTbs(a);}
        op[708] = getOpcodeTrg((char)48, (char)57);
        {char[] a = {48}; op[709] = getOpcodeTbs(a);}
        op[710] = getOpcodeTrg((char)49, (char)57);
        {int[] a = {712,713,714,715,716}; op[711] = getOpcodeAlt(a);}
        op[712] = getOpcodeTrg((char)33, (char)123);
        op[713] = getOpcodeTrg((char)125, (char)126);
        op[714] = getOpcodeRnm(70, 735); // UTF8-2
        op[715] = getOpcodeRnm(71, 738); // UTF8-3
        op[716] = getOpcodeRnm(72, 755); // UTF8-4
        {int[] a = {718,719,720,721,722,723,724,725,726,727}; op[717] = getOpcodeAlt(a);}
        op[718] = getOpcodeRnm(58, 702); // SP
        op[719] = getOpcodeRnm(59, 703); // HTAB
        op[720] = getOpcodeRnm(60, 704); // CR
        op[721] = getOpcodeRnm(61, 705); // LF
        op[722] = getOpcodeTrg((char)32, (char)33);
        op[723] = getOpcodeTrg((char)35, (char)91);
        op[724] = getOpcodeTrg((char)93, (char)126);
        op[725] = getOpcodeRnm(70, 735); // UTF8-2
        op[726] = getOpcodeRnm(71, 738); // UTF8-3
        op[727] = getOpcodeRnm(72, 755); // UTF8-4
        {int[] a = {729,732}; op[728] = getOpcodeAlt(a);}
        {int[] a = {730,731}; op[729] = getOpcodeCat(a);}
        op[730] = getOpcodeRnm(63, 707); // BS
        op[731] = getOpcodeRnm(62, 706); // QM
        {int[] a = {733,734}; op[732] = getOpcodeCat(a);}
        op[733] = getOpcodeRnm(63, 707); // BS
        op[734] = getOpcodeRnm(63, 707); // BS
        {int[] a = {736,737}; op[735] = getOpcodeCat(a);}
        op[736] = getOpcodeTrg((char)194, (char)223);
        op[737] = getOpcodeRnm(73, 770); // UTF8-tail
        {int[] a = {739,743,747,751}; op[738] = getOpcodeAlt(a);}
        {int[] a = {740,741,742}; op[739] = getOpcodeCat(a);}
        {char[] a = {224}; op[740] = getOpcodeTbs(a);}
        op[741] = getOpcodeTrg((char)160, (char)191);
        op[742] = getOpcodeRnm(73, 770); // UTF8-tail
        {int[] a = {744,745}; op[743] = getOpcodeCat(a);}
        op[744] = getOpcodeTrg((char)225, (char)236);
        op[745] = getOpcodeRep((char)2, (char)2, 746);
        op[746] = getOpcodeRnm(73, 770); // UTF8-tail
        {int[] a = {748,749,750}; op[747] = getOpcodeCat(a);}
        {char[] a = {237}; op[748] = getOpcodeTbs(a);}
        op[749] = getOpcodeTrg((char)128, (char)159);
        op[750] = getOpcodeRnm(73, 770); // UTF8-tail
        {int[] a = {752,753}; op[751] = getOpcodeCat(a);}
        op[752] = getOpcodeTrg((char)238, (char)239);
        op[753] = getOpcodeRep((char)2, (char)2, 754);
        op[754] = getOpcodeRnm(73, 770); // UTF8-tail
        {int[] a = {756,761,765}; op[755] = getOpcodeAlt(a);}
        {int[] a = {757,758,759}; op[756] = getOpcodeCat(a);}
        {char[] a = {240}; op[757] = getOpcodeTbs(a);}
        op[758] = getOpcodeTrg((char)144, (char)191);
        op[759] = getOpcodeRep((char)2, (char)2, 760);
        op[760] = getOpcodeRnm(73, 770); // UTF8-tail
        {int[] a = {762,763}; op[761] = getOpcodeCat(a);}
        op[762] = getOpcodeTrg((char)241, (char)243);
        op[763] = getOpcodeRep((char)3, (char)3, 764);
        op[764] = getOpcodeRnm(73, 770); // UTF8-tail
        {int[] a = {766,767,768}; op[765] = getOpcodeCat(a);}
        {char[] a = {244}; op[766] = getOpcodeTbs(a);}
        op[767] = getOpcodeTrg((char)128, (char)143);
        op[768] = getOpcodeRep((char)2, (char)2, 769);
        op[769] = getOpcodeRnm(73, 770); // UTF8-tail
        op[770] = getOpcodeTrg((char)128, (char)191);
        return op;
    }

    public static void display(PrintStream out){
        out.println(";");
        out.println("; org.ihtsdo.rvf.util.LongECLGrammar");
        out.println(";");
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
        out.println("memberOf = \"^\" / (\"m\"/\"M\") (\"e\"/\"E\") (\"m\"/\"M\") (\"b\"/\"B\") (\"e\"/\"E\") (\"r\"/\"R\") (\"o\"/\"O\") (\"f\"/\"F\")");
        out.println("eclConceptReference = conceptId [ws \"|\" ws term ws \"|\"]");
        out.println("conceptId = sctId");
        out.println("term = 1*nonwsNonPipe *( 1*SP 1*nonwsNonPipe )");
        out.println("wildCard = \"*\" / ( (\"a\"/\"A\") (\"n\"/\"N\") (\"y\"/\"Y\") )");
        out.println("constraintOperator = childOf / descendantOrSelfOf / descendantOf / parentOf / ancestorOrSelfOf / ancestorOf");
        out.println("descendantOf = \"<\" / ( (\"d\"/\"D\") (\"e\"/\"E\") (\"s\"/\"S\") (\"c\"/\"C\") (\"e\"/\"E\") (\"n\"/\"N\") (\"d\"/\"D\") (\"a\"/\"A\") (\"n\"/\"N\") (\"t\"/\"T\") (\"o\"/\"O\") (\"f\"/\"F\") mws )");
        out.println("descendantOrSelfOf = \"<<\" / ( (\"d\"/\"D\") (\"e\"/\"E\") (\"s\"/\"S\") (\"c\"/\"C\") (\"e\"/\"E\") (\"n\"/\"N\") (\"d\"/\"D\") (\"a\"/\"A\") (\"n\"/\"N\") (\"t\"/\"T\") (\"o\"/\"O\") (\"r\"/\"R\") (\"s\"/\"S\") (\"e\"/\"E\") (\"l\"/\"L\") (\"f\"/\"F\") (\"o\"/\"O\") (\"f\"/\"F\") mws )");
        out.println("childOf = \"<!\" / ((\"c\"/\"C\") (\"h\"/\"H\") (\"i\"/\"I\") (\"l\"/\"L\") (\"d\"/\"D\") (\"o\"/\"O\") (\"f\"/\"F\") mws )");
        out.println("ancestorOf = \">\" / ( (\"a\"/\"A\") (\"n\"/\"N\") (\"c\"/\"C\") (\"e\"/\"E\") (\"s\"/\"S\") (\"t\"/\"T\") (\"o\"/\"O\") (\"r\"/\"R\") (\"o\"/\"O\") (\"f\"/\"F\") mws )");
        out.println("ancestorOrSelfOf = \">>\" / ( (\"a\"/\"A\") (\"n\"/\"N\") (\"c\"/\"C\") (\"e\"/\"E\") (\"s\"/\"S\") (\"t\"/\"T\") (\"o\"/\"O\") (\"r\"/\"R\") (\"o\"/\"O\") (\"r\"/\"R\") (\"s\"/\"S\") (\"e\"/\"E\") (\"l\"/\"L\") (\"f\"/\"F\") (\"o\"/\"O\") (\"f\"/\"F\") mws )");
        out.println("parentOf = \">!\" / ((\"p\"/\"P\") (\"a\"/\"A\") (\"r\"/\"R\") (\"e\"/\"E\") (\"n\"/\"N\") (\"t\"/\"T\") (\"o\"/\"O\") (\"f\"/\"F\") mws )");
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
        out.println("to = \"..\" / (mws (\"t\"/\"T\") (\"o\"/\"O\") mws)");
        out.println("maxValue = nonNegativeIntegerValue / many");
        out.println("many = \"*\" / ( (\"m\"/\"M\") (\"a\"/\"A\") (\"n\"/\"N\") (\"y\"/\"Y\"))");
        out.println("reverseFlag = ( (\"r\"/\"R\") (\"e\"/\"E\") (\"v\"/\"V\") (\"e\"/\"E\") (\"r\"/\"R\") (\"s\"/\"S\") (\"e\"/\"E\") (\"o\"/\"O\") (\"f\"/\"F\")) / \"R\"");
        out.println("eclAttributeName = subExpressionConstraint");
        out.println("expressionComparisonOperator = \"=\" / \"!=\" / (\"n\"/\"N\") (\"o\"/\"O\") (\"t\"/\"T\") ws \"=\" / \"<>\"");
        out.println("numericComparisonOperator = \"=\" / \"!=\" / (\"n\"/\"N\") (\"o\"/\"O\") (\"t\"/\"T\") ws \"=\" / \"<>\" / \"<=\" / \"<\" / \">=\" / \">\"");
        out.println("stringComparisonOperator = \"=\" / \"!=\" / (\"n\"/\"N\") (\"o\"/\"O\") (\"t\"/\"T\") ws \"=\" / \"<>\"");
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
