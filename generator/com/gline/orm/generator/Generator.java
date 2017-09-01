package com.gline.orm.generator;

import java.util.Arrays;

public class Generator {

	public static void main(String[] args) throws Exception {
		//自定义了一个AddFunction，我们把它定义在包'com.gline.db.generator'中
        String functionImplSourceCode = "package com.gline.db.generator;\n"
                +"import com.gline.db.generator.Function;\n"
                +"public class AddFunction implements Function {\n"
                +"@Override\n"
                +"public int calculate(int x, int y) {\n"
                +"return x + y;\n"
                +"}"
                +"}";
        //创建编译器实例
        StringSourceCompiler compiler = new StringSourceCompiler(Generator.class.getClassLoader(), Arrays.asList("-target", "1.8"));
        //编译代码并得到对应的Class实例
        Class<?> clazz = compiler.compile("com.gline.db.generator.AddFunction", functionImplSourceCode);
        //创建实例
        Object instance = clazz.newInstance();
        Function fun = (Function)instance;
        //执行代码
        int value = fun.calculate(3, 2);
        //输出结果
        System.out.println(value);
    }

}
