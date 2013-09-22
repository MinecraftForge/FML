/*
 * Forge Mod Loader
 * Copyright (c) 2012-2013 Kobata.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 *     Kobata - implementation
 */

package cpw.mods.fml.common.asm.transformers;

import com.google.common.io.Resources;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.net.URL;
import java.util.*;

public class RemappingAccessTransformer implements IClassTransformer {
    private static final int REMOVE_ALL_ACCESS = ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);

    private Set<String> alteredClasses = new HashSet<String>();
    private Map<String, List<MemberInfo>> alterationData = new HashMap<String, List<MemberInfo>>();

    public RemappingAccessTransformer() {
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if(alteredClasses.contains(transformedName)) {
            List<MemberInfo> transforms = alterationData.get(transformedName);
            if(transforms != null) {
                MemberInfo classTransform = null;
                List<MemberInfo> fieldTransforms = new ArrayList<MemberInfo>();
                List<MemberInfo> methodTransforms = new ArrayList<MemberInfo>();

                // Sort transforms by what they apply to for efficiency.
                for(MemberInfo transform : transforms) {
                    switch(transform.type) {
                        case CLASS:
                            classTransform = transform;
                            break;
                        case FIELD:
                            fieldTransforms.add(transform);
                            break;
                        case METHOD:
                            methodTransforms.add(transform);
                            break;
                    }
                }

                ClassReader cr = new ClassReader(bytes);
                ClassNode cn = new ClassNode(Opcodes.ASM4);

                cr.accept(cn, 0);

                if(classTransform != null) {
                    cn.access &= REMOVE_ALL_ACCESS;
                    cn.access |= classTransform.desiredAccess;
                }

                if(cn.fields != null && !fieldTransforms.isEmpty()) {
                    for(FieldNode field : (List<FieldNode>) cn.fields) {
                        String unmappedName = FMLDeobfuscatingRemapper.INSTANCE
                                                                      .mapFieldName(name, field.name, field.desc);

                        for(MemberInfo transform : fieldTransforms) {
                            if(unmappedName.equals(transform.memberName)) {
                                field.access &= REMOVE_ALL_ACCESS;
                                field.access |= transform.desiredAccess;
                                switch(transform.desiredFinal) {
                                    case SETFINAL:
                                        field.access |= Opcodes.ACC_FINAL;
                                        break;
                                    case UNSETFINAL:
                                        field.access &= ~Opcodes.ACC_FINAL;
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                    }
                }

                if(cn.methods != null && !methodTransforms.isEmpty()) {
                    for(MethodNode method : (List<MethodNode>) cn.methods) {
                        String unmappedName = FMLDeobfuscatingRemapper.INSTANCE
                                                                      .mapMethodName(name, method.name, method.desc);
                        String unmappedDesc = FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(method.desc);

                        for(MemberInfo transform : methodTransforms) {
                            if(unmappedName.equals(transform.memberName) && unmappedDesc.equals(transform.memberDesc)) {
                                method.access &= REMOVE_ALL_ACCESS;
                                method.access |= transform.desiredAccess;
                            }
                        }
                    }
                }

                ClassWriter cw = new ClassWriter(0);
                cn.accept(cw);
                return cw.toByteArray();
            }
        }
        return bytes;
    }

    protected void readFile(String fileName) {
        try {
            File file = new File(fileName);
            URL fileURL;
            if(file.exists()) {
                fileURL = file.toURI().toURL();
            } else {
                fileURL = Resources.getResource(fileName);
            }

            Reader input = new BufferedReader(new InputStreamReader(fileURL.openStream()));
            StreamTokenizer tokens = new StreamTokenizer(input);

            tokens.commentChar('#');
            tokens.lowerCaseMode(false);

            tokens.wordChars('a', 'z');
            tokens.wordChars('A', 'Z');
            tokens.wordChars('/', '/');
            tokens.wordChars(';', ';');
            tokens.wordChars('(', ')');
            tokens.wordChars('_', '_');
            tokens.wordChars('.', '.');
            tokens.wordChars('-', '-');


            tokens.whitespaceChars(' ', ' ');
            tokens.whitespaceChars('\t', '\t');

            tokens.eolIsSignificant(true);

            int newAccess = Opcodes.ACC_PUBLIC;
            FinalState finalState = FinalState.UNDEFINED;
            MemberType memberType = MemberType.CLASS;
            String memberInfo = "";
            while(tokens.nextToken() != StreamTokenizer.TT_EOF) {
                if(tokens.ttype == StreamTokenizer.TT_WORD) {
                    if("public".equals(tokens.sval)) {
                        newAccess = Opcodes.ACC_PUBLIC;
                    } else if("protected".equals(tokens.sval)) {
                        newAccess = Opcodes.ACC_PROTECTED;
                    } else if("private".equals(tokens.sval)) {
                        newAccess = Opcodes.ACC_PRIVATE;
                    } else if("class".equals(tokens.sval)) {
                        memberType = MemberType.CLASS;
                    } else if("field".equals(tokens.sval)) {
                        memberType = MemberType.FIELD;
                    } else if("method".equals(tokens.sval)) {
                        memberType = MemberType.METHOD;
                    } else if("final".equals(tokens.sval)) {
                        finalState = FinalState.SETFINAL;
                    } else if("-final".equals(tokens.sval)) {
                        finalState = FinalState.UNSETFINAL;
                    } else {
                        memberInfo = tokens.sval;
                    }
                } else if(tokens.ttype == StreamTokenizer.TT_EOL) {
                    processEntry(newAccess, memberType, memberInfo, finalState);

                    memberInfo = "";
                    finalState = FinalState.UNDEFINED;
                }
            }
        } catch(IOException e) {
            // Do nothing
        }
    }

    private void processEntry(int newAccess, MemberType memberType, String memberInfo, FinalState finalState) {
        if(!memberInfo.isEmpty()) {
            switch(memberType) {
                case CLASS: {
                    alteredClasses.add(memberInfo);
                    List<MemberInfo> info = alterationData.get(memberInfo);

                    if(info == null) {
                        info = new ArrayList<MemberInfo>();
                        alterationData.put(memberInfo, info);
                    }

                    info.add(new MemberInfo(memberInfo, newAccess, finalState));
                    break;
                }
                case FIELD: {
                    int splitPoint = memberInfo.lastIndexOf('.');
                    String className = memberInfo.substring(0, splitPoint);
                    String fieldName = memberInfo.substring(splitPoint + 1);

                    alteredClasses.add(className);
                    List<MemberInfo> info = alterationData.get(className);

                    if(info == null) {
                        info = new ArrayList<MemberInfo>();
                        alterationData.put(className, info);
                    }

                    info.add(new MemberInfo(className, fieldName, newAccess, finalState));
                    break;
                }
                case METHOD: {
                    int splitPoint = memberInfo.lastIndexOf('.');
                    String className = memberInfo.substring(0, splitPoint);
                    String methodNameDesc = memberInfo.substring(splitPoint + 1);

                    splitPoint = methodNameDesc.indexOf('(');
                    String methodName = methodNameDesc.substring(0, splitPoint);
                    String methodDesc = methodNameDesc.substring(splitPoint);

                    alteredClasses.add(className);
                    List<MemberInfo> info = alterationData.get(className);

                    if(info == null) {
                        info = new ArrayList<MemberInfo>();
                        alterationData.put(className, info);
                    }

                    info.add(new MemberInfo(className, methodName, methodDesc, newAccess, finalState));
                    break;
                }
            }
        }
    }

    private enum MemberType {
        CLASS,
        FIELD,
        METHOD
    }

    private enum FinalState {
        UNDEFINED,
        SETFINAL,
        UNSETFINAL
    }

    private class MemberInfo {
        String classType;
        String memberName;
        String memberDesc;
        MemberType type = MemberType.CLASS;
        int desiredAccess = Opcodes.ACC_PUBLIC;
        FinalState desiredFinal = FinalState.UNDEFINED;

        MemberInfo(String className, int access, FinalState finalState) {
            classType = className;
            desiredAccess = access;
            desiredFinal = finalState;
        }

        MemberInfo(String className, String memName, int access, FinalState finalState) {
            classType = className;
            memberName = memName;
            desiredAccess = access;
            desiredFinal = finalState;
            type = MemberType.FIELD;
        }

        MemberInfo(String className, String memName, String memDesc, int access, FinalState finalState) {
            classType = className;
            memberName = memName;
            memberDesc = memDesc;
            desiredAccess = access;
            desiredFinal = finalState;
            type = MemberType.METHOD;
        }
    }
}
