package co.jirm.orm.builder.query;

import java.util.concurrent.atomic.AtomicBoolean;

import co.jirm.core.sql.MutableParameters;
import co.jirm.core.sql.Parameters;
import co.jirm.core.util.SafeAppendable;
import co.jirm.orm.builder.ConditionVisitors;
import co.jirm.orm.builder.ConditionVisitors.ParametersConditionVisitor;


public class SelectClauseVisitors {
	
	public abstract static class SimpleClauseVisitor extends SelectClauseVisitor {

		protected abstract void doVisit(SqlSelectClause<?> clause);
		
		@Override
		public abstract void visit(SelectWhereClauseBuilder<?> whereClauseBuilder);

		@Override
		public void visit(OrderByClauseBuilder<?> clauseBuilder) {
			doVisit(clauseBuilder);
		}

		@Override
		public void visit(LimitClauseBuilder<?> limitClauseBuilder) {
			doVisit(limitClauseBuilder);
		}

		@Override
		public void visit(OffsetClauseBuilder<?> clauseBuilder) {
			doVisit(clauseBuilder);
		}

		@Override
		public void visit(CustomClauseBuilder<?> clauseBuilder) {
			doVisit(clauseBuilder);
		}
		
	}
	
	public abstract static class ParametersClauseVisitor extends SelectClauseVisitor {

		private final ParametersConditionVisitor conditionVisitor = new ParametersConditionVisitor() {
			@Override
			public void doParameters(Parameters p) {
				ParametersClauseVisitor.this.doParameters(p);
			}
		};
		
		@Override
		public void visit(SelectWhereClauseBuilder<?> whereClauseBuilder) {
			whereClauseBuilder.getCondition().accept(conditionVisitor);
		}

		@Override
		public void visit(LimitClauseBuilder<?> limitClauseBuilder) {
			doParameters(limitClauseBuilder);
		}
		
		@Override
		public void visit(OrderByClauseBuilder<?> clauseBuilder) {
			doParameters(clauseBuilder);
		}

		@Override
		public void visit(OffsetClauseBuilder<?> clauseBuilder) {
			doParameters(clauseBuilder);
		}

		@Override
		public void visit(CustomClauseBuilder<?> clauseBuilder) {
			doParameters(clauseBuilder);
		}

		public abstract void doParameters(Parameters p);
		
	}
	
	public static MutableParameters getParameters(SelectVisitorAcceptor k) {
		final MutableParameters mp = new MutableParameters();
		new ParametersClauseVisitor() {
			@Override
			public void doParameters(Parameters p) {
				mp.addAll(p);
			}
		}.startOn(k);
		return mp;
	}
	
	public static SelectClauseVisitor clauseVisitor(final Appendable appendable) {

		final SafeAppendable sb = new SafeAppendable(appendable);
		
		SimpleClauseVisitor cv = new SimpleClauseVisitor() {
			
			AtomicBoolean first = new AtomicBoolean(true);
			
			/*
			 * TODO take care of multiples and order.
			 */
			
			@Override
			protected void doVisit(SqlSelectClause<?> clause) {
				if (! appendStart(clause)) return;
				sb.append(clause.getSql());
			}
			
			@Override
			public void visit(SelectWhereClauseBuilder<?> whereClauseBuilder) {
				if (! appendStart(whereClauseBuilder)) return;
				whereClauseBuilder.getCondition().accept(ConditionVisitors.conditionVisitor(sb.getAppendable()));
			}
			
			private boolean appendStart(SelectClause<?> k) {
				if (k.isNoOp()) return false;
				if (first.get() ) {
					first.set(false);
				}
				else {
					sb.append(" ");
				}
				if (k.getType() != SelectClauseType.CUSTOM)
					sb.append(k.getType().getSql()).append(" ");
				return true;
			}

		};
		return cv;
	}

}
