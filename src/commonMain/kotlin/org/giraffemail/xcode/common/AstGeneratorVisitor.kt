package org.giraffemail.xcode.common

import org.giraffemail.xcode.ast.*

interface AstGeneratorVisitor {
    fun visitModuleNode(node: ModuleNode): String
    fun visitExprNode(node: ExprNode): String
    fun visitPrintNode(node: PrintNode): String
    fun visitFunctionDefNode(node: FunctionDefNode): String
    fun visitClassDefNode(node: ClassDefNode): String
    fun visitAssignNode(node: AssignNode): String
    fun visitCallStatementNode(node: CallStatementNode): String
    fun visitReturnNode(node: ReturnNode): String
    fun visitCallNode(node: CallNode): String
    fun visitNameNode(node: NameNode): String
    fun visitConstantNode(node: ConstantNode): String
    fun visitMemberExpressionNode(node: MemberExpressionNode): String
    fun visitBinaryOpNode(node: BinaryOpNode): String
    fun visitUnaryOpNode(node: UnaryOpNode): String
    fun visitCompareNode(node: CompareNode): String
    fun visitIfNode(node: IfNode): String
    fun visitForLoopNode(node: ForLoopNode): String
    fun visitCStyleForLoopNode(node: CStyleForLoopNode): String
    fun visitListNode(node: ListNode): String
    fun visitTupleNode(node: TupleNode): String
    fun visitUnknownNode(node: UnknownNode): String
}

