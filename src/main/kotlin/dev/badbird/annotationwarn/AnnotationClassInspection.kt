package dev.badbird.annotationwarn;

import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.*

class AnnotationClassInspection : AbstractBaseJavaLocalInspectionTool() {
    private val myQuickFix = AnnotationClassQuickFix()

    fun isAnnotationClass(qualifier: PsiReferenceExpression?): Boolean {
        if (qualifier?.type?.canonicalText?.endsWith("java.lang.annotation.Annotation") == true)
            return true
        if (qualifier?.type?.superTypes != null) {
            if (qualifier.type?.superTypes?.size!! > 0) {
                qualifier.type?.superTypes?.forEach {
                    if (it.canonicalText.endsWith("java.lang.annotation.Annotation"))
                        return true
                }
            }
        }
        return false
    }

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : JavaElementVisitor() {
            // detect calls to Annotation#getClass() and warn
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                // if it ends with .getClass()
                if (expression.methodExpression.text.endsWith(".getClass")) {
                    // check if what it's called on is an annotation
                    val qualifier = expression.methodExpression.qualifier
                    if (qualifier is PsiReferenceExpression && isAnnotationClass(qualifier)) {
                        holder.registerProblem(expression, "Use annotationType() instead", myQuickFix)
                    }
                }
            }
        }
    }

    class AnnotationClassQuickFix : LocalQuickFix {
        override fun getName(): String = "Replace getClass() with annotationType()"
        override fun getFamilyName(): String = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val element = descriptor.psiElement
            if (element is PsiMethodCallExpression) {
                val methodExpression = element.methodExpression
                val qualifier = methodExpression.qualifier as? PsiReferenceExpression
                if (qualifier?.text == "annotation" && methodExpression.text.endsWith(".getClass")) {
                    val newExpressionText = methodExpression.text.replace(".getClass", ".annotationType()")
                    val newExpression = PsiElementFactory.getInstance(project).createExpressionFromText(newExpressionText, element)
                    element.replace(newExpression)
                }
            }
        }
    }
}