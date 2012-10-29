package co.jirm.orm.builder.query;



public abstract class SelectClauseVisitor {
	
	public abstract void visit(SelectWhereClauseBuilder<?> whereClauseBuilder);
	public abstract void visit(OrderByClauseBuilder<?> clauseBuilder);
	public abstract void visit(LimitClauseBuilder<?> limitClauseBuilder);
	public abstract void visit(OffsetClauseBuilder<?> clauseBuilder);
	public abstract void visit(CustomClauseBuilder<?> clauseBuilder);

	
	
	public Object startOn(SelectVisitorAcceptor klause) {
		klause.accept(this);
		return this;
	}
	
}
