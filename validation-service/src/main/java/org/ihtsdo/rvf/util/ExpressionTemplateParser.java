package org.ihtsdo.rvf.util;

import apg.Grammar;
import java.io.PrintStream;

public class ExpressionTemplateParser extends Grammar{

    // public API
    public static Grammar getInstance(){
        if(factoryInstance == null){
            factoryInstance = new ExpressionTemplateParser(getRules(), getUdts(), getOpcodes());
        }
        return factoryInstance;
    }

    // rule name enum
    public static int ruleCount = 50;
    public enum RuleNames{
        ANYNONESCAPEDCHAR("anyNonEscapedChar", 32, 154, 10),
        ATTRIBUTE("attribute", 12, 79, 10),
        ATTRIBUTEGROUP("attributeGroup", 10, 61, 10),
        ATTRIBUTENAME("attributeName", 13, 89, 1),
        ATTRIBUTESET("attributeSet", 11, 71, 8),
        ATTRIBUTEVALUE("attributeValue", 14, 90, 9),
        BS("BS", 27, 144, 1),
        CONCEPTID("conceptId", 7, 41, 1),
        CONCEPTREFERENCE("conceptReference", 6, 29, 12),
        CR("CR", 24, 141, 1),
        DECIMALVALUE("decimalValue", 19, 124, 5),
        DEFINITIONSTATUS("definitionStatus", 2, 16, 3),
        DIGIT("digit", 28, 145, 1),
        DIGITNONZERO("digitNonZero", 30, 147, 1),
        EQUIVALENTTO("equivalentTo", 3, 19, 1),
        ESCAPEDCHAR("escapedChar", 33, 164, 7),
        EXPRESSIONTEMPLATE("expressionTemplate", 0, 0, 8),
        EXPRESSIONVALUE("expressionValue", 15, 99, 8),
        FOCUSCONCEPT("focusConcept", 5, 21, 8),
        HTAB("HTAB", 23, 140, 1),
        INTEGERVALUE("integerValue", 18, 114, 10),
        LF("LF", 25, 142, 1),
        NONDOUBLEQUOTESTRING("nondoubleQuoteString", 47, 279, 5),
        NONQUOTESTRING("nonQuoteString", 46, 271, 8),
        NONSINGLEQUOTESTRING("nonsingleQuoteString", 48, 284, 5),
        NONWSNONPIPE("nonwsNonPipe", 31, 148, 6),
        NUMERICVALUE("numericValue", 17, 111, 3),
        QM("QM", 26, 143, 1),
        REFINEMENT("refinement", 9, 49, 12),
        REPLACEFLAG("replaceFlag", 41, 235, 4),
        REPLACEINFO("replaceInfo", 40, 227, 8),
        SCTID("sctId", 20, 129, 4),
        SP("SP", 22, 139, 1),
        SQM("SQM", 49, 289, 1),
        STRINGVALUE("stringValue", 16, 107, 4),
        SUBEXPRESSION("subExpression", 1, 8, 8),
        SUBTYPEOF("subtypeOf", 4, 20, 1),
        TEMPLATEREMOVESLOT("templateRemoveSlot", 39, 219, 8),
        TEMPLATEREPLACESLOT("templateReplaceSlot", 38, 207, 12),
        TEMPLATESLOTINFO("templateSlotInfo", 42, 239, 13),
        TEMPLATESLOTNAME("templateSlotName", 43, 252, 3),
        TEMPLATESLOTREFERENCE("templateSlotReference", 44, 255, 3),
        TEMPLATESTRING("templateString", 45, 258, 13),
        TERM("term", 8, 42, 7),
        UTF8_2("UTF8-2", 34, 171, 3),
        UTF8_3("UTF8-3", 35, 174, 17),
        UTF8_4("UTF8-4", 36, 191, 15),
        UTF8_TAIL("UTF8-tail", 37, 206, 1),
        WS("ws", 21, 133, 6),
        ZERO("zero", 29, 146, 1);
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
    private static ExpressionTemplateParser factoryInstance = null;
    private ExpressionTemplateParser(Rule[] rules, Udt[] udts, Opcode[] opcodes){
        super(rules, udts, opcodes);
    }

    private static Rule[] getRules(){
    	Rule[] rules = new Rule[50];
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
    	Opcode[] op = new Opcode[290];
        {int[] a = {1,2,6,7}; op[0] = getOpcodeCat(a);}
        op[1] = getOpcodeRnm(21, 133); // ws
        op[2] = getOpcodeRep((char)0, (char)1, 3);
        {int[] a = {4,5}; op[3] = getOpcodeCat(a);}
        op[4] = getOpcodeRnm(2, 16); // definitionStatus
        op[5] = getOpcodeRnm(21, 133); // ws
        op[6] = getOpcodeRnm(1, 8); // subExpression
        op[7] = getOpcodeRnm(21, 133); // ws
        {int[] a = {9,10}; op[8] = getOpcodeCat(a);}
        op[9] = getOpcodeRnm(5, 21); // focusConcept
        op[10] = getOpcodeRep((char)0, (char)1, 11);
        {int[] a = {12,13,14,15}; op[11] = getOpcodeCat(a);}
        op[12] = getOpcodeRnm(21, 133); // ws
        {char[] a = {58}; op[13] = getOpcodeTls(a);}
        op[14] = getOpcodeRnm(21, 133); // ws
        op[15] = getOpcodeRnm(9, 49); // refinement
        {int[] a = {17,18}; op[16] = getOpcodeAlt(a);}
        op[17] = getOpcodeRnm(3, 19); // equivalentTo
        op[18] = getOpcodeRnm(4, 20); // subtypeOf
        {char[] a = {61,61,61}; op[19] = getOpcodeTls(a);}
        {char[] a = {60,60,60}; op[20] = getOpcodeTls(a);}
        {int[] a = {22,23}; op[21] = getOpcodeCat(a);}
        op[22] = getOpcodeRnm(6, 29); // conceptReference
        op[23] = getOpcodeRep((char)0, Character.MAX_VALUE, 24);
        {int[] a = {25,26,27,28}; op[24] = getOpcodeCat(a);}
        op[25] = getOpcodeRnm(21, 133); // ws
        {char[] a = {43}; op[26] = getOpcodeTls(a);}
        op[27] = getOpcodeRnm(21, 133); // ws
        op[28] = getOpcodeRnm(6, 29); // conceptReference
        {int[] a = {30,31}; op[29] = getOpcodeAlt(a);}
        op[30] = getOpcodeRnm(38, 207); // templateReplaceSlot
        {int[] a = {32,33}; op[31] = getOpcodeCat(a);}
        op[32] = getOpcodeRnm(7, 41); // conceptId
        op[33] = getOpcodeRep((char)0, (char)1, 34);
        {int[] a = {35,36,37,38,39,40}; op[34] = getOpcodeCat(a);}
        op[35] = getOpcodeRnm(21, 133); // ws
        {char[] a = {124}; op[36] = getOpcodeTls(a);}
        op[37] = getOpcodeRnm(21, 133); // ws
        op[38] = getOpcodeRnm(8, 42); // term
        op[39] = getOpcodeRnm(21, 133); // ws
        {char[] a = {124}; op[40] = getOpcodeTls(a);}
        op[41] = getOpcodeRnm(20, 129); // sctId
        {int[] a = {43,44}; op[42] = getOpcodeCat(a);}
        op[43] = getOpcodeRnm(31, 148); // nonwsNonPipe
        op[44] = getOpcodeRep((char)0, Character.MAX_VALUE, 45);
        {int[] a = {46,48}; op[45] = getOpcodeCat(a);}
        op[46] = getOpcodeRep((char)0, Character.MAX_VALUE, 47);
        op[47] = getOpcodeRnm(22, 139); // SP
        op[48] = getOpcodeRnm(31, 148); // nonwsNonPipe
        {int[] a = {50,53}; op[49] = getOpcodeCat(a);}
        {int[] a = {51,52}; op[50] = getOpcodeAlt(a);}
        op[51] = getOpcodeRnm(11, 71); // attributeSet
        op[52] = getOpcodeRnm(10, 61); // attributeGroup
        op[53] = getOpcodeRep((char)0, Character.MAX_VALUE, 54);
        {int[] a = {55,56,60}; op[54] = getOpcodeCat(a);}
        op[55] = getOpcodeRnm(21, 133); // ws
        op[56] = getOpcodeRep((char)0, (char)1, 57);
        {int[] a = {58,59}; op[57] = getOpcodeCat(a);}
        {char[] a = {44}; op[58] = getOpcodeTls(a);}
        op[59] = getOpcodeRnm(21, 133); // ws
        op[60] = getOpcodeRnm(10, 61); // attributeGroup
        {int[] a = {62,66,67,68,69,70}; op[61] = getOpcodeCat(a);}
        op[62] = getOpcodeRep((char)0, (char)1, 63);
        {int[] a = {64,65}; op[63] = getOpcodeCat(a);}
        op[64] = getOpcodeRnm(39, 219); // templateRemoveSlot
        op[65] = getOpcodeRnm(21, 133); // ws
        {char[] a = {123}; op[66] = getOpcodeTls(a);}
        op[67] = getOpcodeRnm(21, 133); // ws
        op[68] = getOpcodeRnm(11, 71); // attributeSet
        op[69] = getOpcodeRnm(21, 133); // ws
        {char[] a = {125}; op[70] = getOpcodeTls(a);}
        {int[] a = {72,73}; op[71] = getOpcodeCat(a);}
        op[72] = getOpcodeRnm(12, 79); // attribute
        op[73] = getOpcodeRep((char)0, Character.MAX_VALUE, 74);
        {int[] a = {75,76,77,78}; op[74] = getOpcodeCat(a);}
        op[75] = getOpcodeRnm(21, 133); // ws
        {char[] a = {44}; op[76] = getOpcodeTls(a);}
        op[77] = getOpcodeRnm(21, 133); // ws
        op[78] = getOpcodeRnm(12, 79); // attribute
        {int[] a = {80,84,85,86,87,88}; op[79] = getOpcodeCat(a);}
        op[80] = getOpcodeRep((char)0, (char)1, 81);
        {int[] a = {82,83}; op[81] = getOpcodeCat(a);}
        op[82] = getOpcodeRnm(39, 219); // templateRemoveSlot
        op[83] = getOpcodeRnm(21, 133); // ws
        op[84] = getOpcodeRnm(13, 89); // attributeName
        op[85] = getOpcodeRnm(21, 133); // ws
        {char[] a = {61}; op[86] = getOpcodeTls(a);}
        op[87] = getOpcodeRnm(21, 133); // ws
        op[88] = getOpcodeRnm(14, 90); // attributeValue
        op[89] = getOpcodeRnm(0, 0); // expressionTemplate
        {int[] a = {91,92,96}; op[90] = getOpcodeAlt(a);}
        op[91] = getOpcodeRnm(15, 99); // expressionValue
        {int[] a = {93,94,95}; op[92] = getOpcodeCat(a);}
        op[93] = getOpcodeRnm(26, 143); // QM
        op[94] = getOpcodeRnm(16, 107); // stringValue
        op[95] = getOpcodeRnm(26, 143); // QM
        {int[] a = {97,98}; op[96] = getOpcodeCat(a);}
        {char[] a = {35}; op[97] = getOpcodeTls(a);}
        op[98] = getOpcodeRnm(17, 111); // numericValue
        {int[] a = {100,101}; op[99] = getOpcodeAlt(a);}
        op[100] = getOpcodeRnm(6, 29); // conceptReference
        {int[] a = {102,103,104,105,106}; op[101] = getOpcodeCat(a);}
        {char[] a = {40}; op[102] = getOpcodeTls(a);}
        op[103] = getOpcodeRnm(21, 133); // ws
        op[104] = getOpcodeRnm(1, 8); // subExpression
        op[105] = getOpcodeRnm(21, 133); // ws
        {char[] a = {41}; op[106] = getOpcodeTls(a);}
        op[107] = getOpcodeRep((char)1, Character.MAX_VALUE, 108);
        {int[] a = {109,110}; op[108] = getOpcodeAlt(a);}
        op[109] = getOpcodeRnm(32, 154); // anyNonEscapedChar
        op[110] = getOpcodeRnm(33, 164); // escapedChar
        {int[] a = {112,113}; op[111] = getOpcodeAlt(a);}
        op[112] = getOpcodeRnm(19, 124); // decimalValue
        op[113] = getOpcodeRnm(18, 114); // integerValue
        {int[] a = {115,123}; op[114] = getOpcodeAlt(a);}
        {int[] a = {116,120,121}; op[115] = getOpcodeCat(a);}
        op[116] = getOpcodeRep((char)0, (char)1, 117);
        {int[] a = {118,119}; op[117] = getOpcodeAlt(a);}
        {char[] a = {45}; op[118] = getOpcodeTls(a);}
        {char[] a = {43}; op[119] = getOpcodeTls(a);}
        op[120] = getOpcodeRnm(30, 147); // digitNonZero
        op[121] = getOpcodeRep((char)0, Character.MAX_VALUE, 122);
        op[122] = getOpcodeRnm(28, 145); // digit
        op[123] = getOpcodeRnm(29, 146); // zero
        {int[] a = {125,126,127}; op[124] = getOpcodeCat(a);}
        op[125] = getOpcodeRnm(18, 114); // integerValue
        {char[] a = {46}; op[126] = getOpcodeTls(a);}
        op[127] = getOpcodeRep((char)1, Character.MAX_VALUE, 128);
        op[128] = getOpcodeRnm(28, 145); // digit
        {int[] a = {130,131}; op[129] = getOpcodeCat(a);}
        op[130] = getOpcodeRnm(30, 147); // digitNonZero
        op[131] = getOpcodeRep((char)5, (char)17, 132);
        op[132] = getOpcodeRnm(28, 145); // digit
        op[133] = getOpcodeRep((char)0, Character.MAX_VALUE, 134);
        {int[] a = {135,136,137,138}; op[134] = getOpcodeAlt(a);}
        op[135] = getOpcodeRnm(22, 139); // SP
        op[136] = getOpcodeRnm(23, 140); // HTAB
        op[137] = getOpcodeRnm(24, 141); // CR
        op[138] = getOpcodeRnm(25, 142); // LF
        {char[] a = {32}; op[139] = getOpcodeTbs(a);}
        {char[] a = {9}; op[140] = getOpcodeTbs(a);}
        {char[] a = {13}; op[141] = getOpcodeTbs(a);}
        {char[] a = {10}; op[142] = getOpcodeTbs(a);}
        {char[] a = {34}; op[143] = getOpcodeTbs(a);}
        {char[] a = {92}; op[144] = getOpcodeTbs(a);}
        op[145] = getOpcodeTrg((char)48, (char)57);
        {char[] a = {48}; op[146] = getOpcodeTbs(a);}
        op[147] = getOpcodeTrg((char)49, (char)3);
        {int[] a = {149,150,151,152,153}; op[148] = getOpcodeAlt(a);}
        op[149] = getOpcodeTrg((char)33, (char)123);
        op[150] = getOpcodeTrg((char)125, (char)126);
        op[151] = getOpcodeRnm(34, 171); // UTF8-2
        op[152] = getOpcodeRnm(35, 174); // UTF8-3
        op[153] = getOpcodeRnm(36, 191); // UTF8-4
        {int[] a = {155,156,157,158,159,160,161,162,163}; op[154] = getOpcodeAlt(a);}
        op[155] = getOpcodeRnm(23, 140); // HTAB
        op[156] = getOpcodeRnm(24, 141); // CR
        op[157] = getOpcodeRnm(25, 142); // LF
        op[158] = getOpcodeTrg((char)32, (char)33);
        op[159] = getOpcodeTrg((char)35, (char)91);
        op[160] = getOpcodeTrg((char)93, (char)126);
        op[161] = getOpcodeRnm(34, 171); // UTF8-2
        op[162] = getOpcodeRnm(35, 174); // UTF8-3
        op[163] = getOpcodeRnm(36, 191); // UTF8-4
        {int[] a = {165,168}; op[164] = getOpcodeAlt(a);}
        {int[] a = {166,167}; op[165] = getOpcodeCat(a);}
        op[166] = getOpcodeRnm(27, 144); // BS
        op[167] = getOpcodeRnm(26, 143); // QM
        {int[] a = {169,170}; op[168] = getOpcodeCat(a);}
        op[169] = getOpcodeRnm(27, 144); // BS
        op[170] = getOpcodeRnm(27, 144); // BS
        {int[] a = {172,173}; op[171] = getOpcodeCat(a);}
        op[172] = getOpcodeTrg((char)194, (char)223);
        op[173] = getOpcodeRnm(37, 206); // UTF8-tail
        {int[] a = {175,179,183,187}; op[174] = getOpcodeAlt(a);}
        {int[] a = {176,177,178}; op[175] = getOpcodeCat(a);}
        {char[] a = {224}; op[176] = getOpcodeTbs(a);}
        op[177] = getOpcodeTrg((char)160, (char)191);
        op[178] = getOpcodeRnm(37, 206); // UTF8-tail
        {int[] a = {180,181}; op[179] = getOpcodeCat(a);}
        op[180] = getOpcodeTrg((char)225, (char)236);
        op[181] = getOpcodeRep((char)2, (char)2, 182);
        op[182] = getOpcodeRnm(37, 206); // UTF8-tail
        {int[] a = {184,185,186}; op[183] = getOpcodeCat(a);}
        {char[] a = {237}; op[184] = getOpcodeTbs(a);}
        op[185] = getOpcodeTrg((char)128, (char)159);
        op[186] = getOpcodeRnm(37, 206); // UTF8-tail
        {int[] a = {188,189}; op[187] = getOpcodeCat(a);}
        op[188] = getOpcodeTrg((char)238, (char)239);
        op[189] = getOpcodeRep((char)2, (char)2, 190);
        op[190] = getOpcodeRnm(37, 206); // UTF8-tail
        {int[] a = {192,197,201}; op[191] = getOpcodeAlt(a);}
        {int[] a = {193,194,195}; op[192] = getOpcodeCat(a);}
        {char[] a = {240}; op[193] = getOpcodeTbs(a);}
        op[194] = getOpcodeTrg((char)144, (char)191);
        op[195] = getOpcodeRep((char)2, (char)2, 196);
        op[196] = getOpcodeRnm(37, 206); // UTF8-tail
        {int[] a = {198,199}; op[197] = getOpcodeCat(a);}
        op[198] = getOpcodeTrg((char)241, (char)243);
        op[199] = getOpcodeRep((char)3, (char)3, 200);
        op[200] = getOpcodeRnm(37, 206); // UTF8-tail
        {int[] a = {202,203,204}; op[201] = getOpcodeCat(a);}
        {char[] a = {244}; op[202] = getOpcodeTbs(a);}
        op[203] = getOpcodeTrg((char)128, (char)143);
        op[204] = getOpcodeRep((char)2, (char)2, 205);
        op[205] = getOpcodeRnm(37, 206); // UTF8-tail
        op[206] = getOpcodeTrg((char)128, (char)11);
        {int[] a = {208,209,210,211,212,216,218}; op[207] = getOpcodeCat(a);}
        {char[] a = {91,91}; op[208] = getOpcodeTls(a);}
        op[209] = getOpcodeRnm(21, 133); // ws
        {char[] a = {43}; op[210] = getOpcodeTls(a);}
        op[211] = getOpcodeRnm(21, 133); // ws
        op[212] = getOpcodeRep((char)0, (char)1, 213);
        {int[] a = {214,215}; op[213] = getOpcodeCat(a);}
        op[214] = getOpcodeRnm(40, 227); // replaceInfo
        op[215] = getOpcodeRnm(21, 133); // ws
        op[216] = getOpcodeRep((char)0, (char)1, 217);
        op[217] = getOpcodeRnm(42, 239); // templateSlotInfo
        {char[] a = {93,93}; op[218] = getOpcodeTls(a);}
        {int[] a = {220,221,222,223,224,226}; op[219] = getOpcodeCat(a);}
        {char[] a = {91,91}; op[220] = getOpcodeTls(a);}
        op[221] = getOpcodeRnm(21, 133); // ws
        {char[] a = {126}; op[222] = getOpcodeTls(a);}
        op[223] = getOpcodeRnm(21, 133); // ws
        op[224] = getOpcodeRep((char)0, (char)1, 225);
        op[225] = getOpcodeRnm(42, 239); // templateSlotInfo
        {char[] a = {93,93}; op[226] = getOpcodeTls(a);}
        {int[] a = {228,229}; op[227] = getOpcodeCat(a);}
        op[228] = getOpcodeRnm(41, 235); // replaceFlag
        op[229] = getOpcodeRep((char)0, (char)1, 230);
        {int[] a = {231,232,233,234}; op[230] = getOpcodeCat(a);}
        op[231] = getOpcodeRnm(21, 133); // ws
        {char[] a = {40}; op[232] = getOpcodeTls(a);}
        op[233] = getOpcodeRnm(0, 0); // expressionTemplate
        {char[] a = {41}; op[234] = getOpcodeTls(a);}
        {int[] a = {236,237,238}; op[235] = getOpcodeAlt(a);}
        {char[] a = {105,100}; op[236] = getOpcodeTls(a);}
        {char[] a = {115,99,103}; op[237] = getOpcodeTls(a);}
        {char[] a = {101,99,108}; op[238] = getOpcodeTls(a);}
        {int[] a = {240,244,248}; op[239] = getOpcodeCat(a);}
        op[240] = getOpcodeRep((char)0, (char)1, 241);
        {int[] a = {242,243}; op[241] = getOpcodeCat(a);}
        op[242] = getOpcodeRnm(0, 0); // expressionTemplate
        op[243] = getOpcodeRnm(21, 133); // ws
        op[244] = getOpcodeRep((char)0, (char)1, 245);
        {int[] a = {246,247}; op[245] = getOpcodeCat(a);}
        op[246] = getOpcodeRnm(43, 252); // templateSlotName
        op[247] = getOpcodeRnm(21, 133); // ws
        op[248] = getOpcodeRep((char)0, (char)1, 249);
        {int[] a = {250,251}; op[249] = getOpcodeCat(a);}
        op[250] = getOpcodeRnm(44, 255); // templateSlotReference
        op[251] = getOpcodeRnm(21, 133); // ws
        {int[] a = {253,254}; op[252] = getOpcodeCat(a);}
        {char[] a = {64}; op[253] = getOpcodeTls(a);}
        op[254] = getOpcodeRnm(45, 258); // templateString
        {int[] a = {256,257}; op[255] = getOpcodeCat(a);}
        {char[] a = {36}; op[256] = getOpcodeTls(a);}
        op[257] = getOpcodeRnm(45, 258); // templateString
        {int[] a = {259,261,266}; op[258] = getOpcodeAlt(a);}
        op[259] = getOpcodeRep((char)0, Character.MAX_VALUE, 260);
        op[260] = getOpcodeRnm(46, 271); // nonQuoteString
        {int[] a = {262,263,265}; op[261] = getOpcodeCat(a);}
        op[262] = getOpcodeRnm(26, 143); // QM
        op[263] = getOpcodeRep((char)0, Character.MAX_VALUE, 264);
        op[264] = getOpcodeRnm(47, 279); // nondoubleQuoteString
        op[265] = getOpcodeRnm(26, 143); // QM
        {int[] a = {267,268,270}; op[266] = getOpcodeCat(a);}
        op[267] = getOpcodeRnm(49, 289); // SQM
        op[268] = getOpcodeRep((char)0, Character.MAX_VALUE, 269);
        op[269] = getOpcodeRnm(48, 284); // nonsingleQuoteString
        op[270] = getOpcodeRnm(49, 289); // SQM
        {int[] a = {272,273,274,275,276,277,278}; op[271] = getOpcodeAlt(a);}
        {char[] a = {33}; op[272] = getOpcodeTbs(a);}
        {char[] a = {35}; op[273] = getOpcodeTbs(a);}
        op[274] = getOpcodeTrg((char)37, (char)38);
        op[275] = getOpcodeTrg((char)40, (char)63);
        op[276] = getOpcodeTrg((char)65, (char)90);
        {char[] a = {92}; op[277] = getOpcodeTbs(a);}
        op[278] = getOpcodeTrg((char)94, (char)126);
        {int[] a = {280,281,282,283}; op[279] = getOpcodeAlt(a);}
        {char[] a = {33}; op[280] = getOpcodeTbs(a);}
        {char[] a = {35}; op[281] = getOpcodeTbs(a);}
        op[282] = getOpcodeTrg((char)37, (char)63);
        op[283] = getOpcodeTrg((char)65, (char)126);
        {int[] a = {285,286,287,288}; op[284] = getOpcodeAlt(a);}
        op[285] = getOpcodeTrg((char)33, (char)35);
        op[286] = getOpcodeTrg((char)37, (char)38);
        op[287] = getOpcodeTrg((char)40, (char)63);
        op[288] = getOpcodeTrg((char)65, (char)126);
        {char[] a = {2}; op[289] = getOpcodeTbs(a);}
        return op;
    }

    public static void display(PrintStream out){
        out.println(";");
        out.println("; package.name.ExpressionTemplateParser");
        out.println(";");
        out.println(";********************************************************************");
        out.println(";  APG - an ABNF Parser Generator");
        out.println(";  Copyright (C) 2011 Lowell D. Thomas, all rights reserved");
        out.println(";");
        out.println(";    author: Lowell D. Thomas");
        out.println(";            lowell@coasttocoastresearch.com");
        out.println(";            http://www.coasttocoastresearch.com");
        out.println(";");
        out.println(";   purpose: ABNF for SABNF");
        out.println(";");
        out.println(";*********************************************************************");
        out.println("; symbol alphabet is ASCII");
        out.println("; code points: 9, 10, 13, 32-126");
        out.println(";");
        out.println("expressionTemplate = ws [definitionStatus ws] subExpression ws");
        out.println("subExpression = focusConcept [ws \":\" ws refinement]");
        out.println("definitionStatus = equivalentTo/subtypeOf");
        out.println("equivalentTo = \"===\"");
        out.println("subtypeOf = \"<<<\"");
        out.println("focusConcept = conceptReference *(ws \"+\" ws conceptReference)");
        out.println("conceptReference = templateReplaceSlot/(conceptId [ws \"|\" ws term ws \"|\"])");
        out.println("conceptId = sctId");
        out.println("term = nonwsNonPipe *(*SP nonwsNonPipe)");
        out.println("refinement = (attributeSet/attributeGroup) *( ws [\",\" ws] attributeGroup )");
        out.println("attributeGroup = [templateRemoveSlot ws] \"{\" ws attributeSet ws \"}\"");
        out.println("attributeSet = attribute *(ws \",\" ws attribute)");
        out.println("attribute = [  templateRemoveSlot ws] attributeName ws \"=\" ws attributeValue");
        out.println("attributeName=   conceptReferenc");
        out.println("attributeValue=  expressionValue / QM stringValue QM /   \"#\" numericValue");
        out.println("expressionValue= conceptReference /   \"(\" ws subExpression ws \")\"");
        out.println("stringValue=   1*(anyNonEscapedChar /   escapedChar)");
        out.println("numericValue= decimalValue / integerValue");
        out.println("integerValue=   ([\"-\"/\"+\"] digitNonZero *digit )  /   zero");
        out.println("decimalValue=   integerValue  \".\" 1*digit");
        out.println("sctId=   digitNonZero 5*17( digit )");
        out.println("ws=   *( SP /   HTAB / CR /   LF )  ;  optional white space");
        out.println("SP=   %x20 ;  space");
        out.println("HTAB=   %x09 ; tab");
        out.println("CR=   %x0D ;  carriage return");
        out.println("LF=   %x0A ; line feed");
        out.println("QM= %x22  ;  quotation mark");
        out.println("BS=   %x5C  ; back slash");
        out.println("digit= %x30-39");
        out.println("zero= %x30");
        out.println("digitNonZero=   %x31-3");
        out.println("nonwsNonPipe=   %x21-7B / %x7D-7E /   UTF8-2 /   UTF8-3 / UTF8-4");
        out.println("anyNonEscapedChar=   HTAB /   CR /   LF /   %x20-21 /   %x23-5B /   %x5D-7E /   UTF8-2 / UTF8-3 / UTF8-4");
        out.println("escapedChar= BS QM /  BS BS");
        out.println("UTF8-2= %xC2-DF UTF8-tail");
        out.println("UTF8-3= %xE0 %xA0-BF UTF8-tail /   %xE1-EC 2( UTF8-tail )  /   %xED %x80-9F UTF8-tail / %xEE-EF 2( UTF8-tail )");
        out.println("UTF8-4= %xF0 %x90-BF 2( UTF8-tail ) /   %xF1-F3 3( UTF8-tail )  / %xF4 %x80-8F 2( UTF8-tail )");
        out.println("UTF8-tail=   %x80-B");
        out.println("");
        out.println(";  The second part of the syntax uses the draft SNOMED CT Template Syntax v0.1 rules");
        out.println("templateReplaceSlot= \"[[\" ws \"+\" ws [replaceInfo ws] [templateSlotInfo] \"]]\"");
        out.println("templateRemoveSlot=   \"[[\" ws \"~\" ws [templateSlotInfo] \"]]\"");
        out.println("replaceInfo= replaceFlag [ws \"(\" expressionConstraintTemplate \")\"]");
        out.println("replaceFlag= \"id\" /   \"scg\" /   \"ecl\"");
        out.println("templateSlotInfo=   [cardinality ws] [templateSlotName ws] [templateSlotReference ws]");
        out.println("templateSlotName=   \"@\" templateString");
        out.println("templateSlotReference= \"$\" templateString");
        out.println("templateString= *nonQuoteString /(QM *nondoubleQuoteString QM) / (SQM *nonsingleQuoteString SQM)");
        out.println("nonQuoteString= %x21 /   %x23 /   %x25-26 / %x28-3F /   %x41-5A /   %x5C / %x5E-7E ; no ws, quotes, at or dollar");
        out.println("nondoubleQuoteString=   %x21 /   %x23 / %x25-3F /   %x41-7E ; no ws, double quotes, at or dollar");
        out.println("nonsingleQuoteString=   %x21-23 /   %x25-26 / %x28-3F /   %x41-7E ; no ws, single quotes, at or dollar");
        out.println("SQM=   %x2");
    }
}
