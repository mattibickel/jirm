package co.jirm.mapper.definition;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import co.jirm.mapper.SqlObjectConfig;
import co.jirm.mapper.converter.SqlParameterConverter;

import com.google.common.base.Optional;

public class SqlParameterDefinition {
	private final String parameterName;
	private final String sqlName;
	private final Class<?> parameterType;
	private final int order;
	private final boolean id;
	private final Optional<SqlParameterObjectDefinition> objectDefinition;
	private final SqlParameterConverter parameterConverter;
	
	
	SqlParameterDefinition(SqlParameterConverter parameterConverter, String parameterName, Class<?> parameterType, int order, String sqlName, boolean id) {
		super();
		this.parameterName = parameterName;
		this.parameterType = parameterType;
		this.order = order;
		this.sqlName = sqlName;
		this.id = id;
		this.objectDefinition = Optional.absent();
		this.parameterConverter = parameterConverter;
		
	}
	
	SqlParameterDefinition(
			SqlParameterConverter parameterConverter,
			String parameterName,
			@Nonnull
			SqlParameterObjectDefinition objDef, 
			int order, String sqlName) {
		this.objectDefinition = Optional.<SqlParameterObjectDefinition>of(objDef);
		this.parameterName = parameterName;
		this.parameterType = objectDefinition.get().getObjectDefintion().getObjectType();
		this.order = order;
		this.sqlName = sqlName;
		this.id = false;
		this.parameterConverter = parameterConverter;
		
	}
	
	public Object convertToSql(Object original) {
		return this.parameterConverter.convertToSql(original, this);
	}
	
	static Map<String, SqlParameterDefinition> getSqlBeanParameters(Class<?> k, SqlObjectConfig config) {
		Map<String, SqlParameterDefinition> parameters = new LinkedHashMap<String, SqlParameterDefinition>();
		Constructor<?> cons[] = k.getConstructors();
		for (Constructor<?> c : cons) {
			JsonCreator jc = c.getAnnotation(JsonCreator.class);
			if (jc == null) continue;
			Annotation[][] aas = c.getParameterAnnotations();
			Class<?>[] pts = c.getParameterTypes();
			if (aas == null || aas.length == 0) continue;
			for (int i = 0; i < aas.length; i++) {
				Annotation[] as = aas[i];
				Class<?> parameterType = pts[i];
				for (int j = 0; j < as.length; j++) {
					Annotation a = as[j];
					//TODO handle http://docs.oracle.com/javase/6/docs/api/java/beans/ConstructorProperties.html
					//https://github.com/joshbeitelspacher/jackson-extensions/blob/master/src/main/java/com/netbeetle/jackson/ConstructorPropertiesAnnotationIntrospector.java
					if (JsonProperty.class.equals(a.annotationType())) {
						JsonProperty p = (JsonProperty) a;
						String value = p.value();
						final SqlParameterDefinition definition = parameterDef(config, k, value, parameterType, i);
						parameters.put(value, definition);
					}
				}
			}
		}
		return parameters;
	}
	
	
	public Optional<SqlParameterObjectDefinition> getObjectDefinition() {
		return objectDefinition;
	}

	private static SqlParameterDefinition parameterDef(
			SqlObjectConfig config,
			Class<?> objectType, 
			String parameterName, 
			Class<?> parameterType, int order) {
		
		final SqlParameterDefinition definition;
		String sn = null;
		ManyToOne manyToOne = getAnnotation(objectType, parameterName, ManyToOne.class);
		if (manyToOne != null) {
			Class<?> subK = checkNotNull(manyToOne.targetEntity(), "targetEntity not set");
			JoinColumn joinColumn = getAnnotation(objectType, parameterName, JoinColumn.class);
			SqlObjectDefinition<?> od = SqlObjectDefinition.fromClass(subK, config);
			checkState( ! od.getIdParameters().isEmpty(), "No id parameters");
			if (joinColumn != null)
				sn = joinColumn.name();
			if (sn == null)
				sn = config.getNamingStrategy().propertyToColumnName(parameterName);
			FetchType fetch = manyToOne.fetch();
			int depth;
			if (FetchType.LAZY == fetch) {
				depth = 1;
			}
			else {
				depth = config.getMaximumLoadDepth();
				
			}
			SqlParameterObjectDefinition sod = new SqlParameterObjectDefinition(od, depth);
			definition = new SqlParameterDefinition(config.getConverter(), parameterName, sod, order, sn);
		}
		else {
			Column col = getAnnotation(objectType, parameterName, Column.class);
			if (col != null)
				sn = col.name();
			Id id = getAnnotation(objectType, parameterName, Id.class);
			boolean idFlag = id != null;
			if (sn == null)
				sn = config.getNamingStrategy().propertyToColumnName(parameterName);
			definition = new SqlParameterDefinition(config.getConverter(), parameterName, parameterType, order, sn, idFlag);
		}
		return definition;
	}

	private static <T extends Annotation> T getAnnotation(Class<?> k, String value, Class<T> a) {
		try {
			Field f = k.getDeclaredField(value);
			return f.getAnnotation(a);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getParameterName() {
		return parameterName;
	}
	public Class<?> getParameterType() {
		return parameterType;
	}
	public int getOrder() {
		return order;
	}
	public String sqlName() {
		return sqlName;
	}
	public String sqlName(String prefix) {
		if (prefix != null)
			return prefix + "." + sqlName;
		return sqlName;
	}
	
	public boolean isId() {
		return id;
	}
	public boolean isComplex() {
		return this.getObjectDefinition().isPresent();
	}
	
	//@SuppressWarnings("unchecked")
	public Optional<Object> valueFrom(Map<String,Object> m) {
		Object o = m.get(this.getParameterName());
		return Optional.fromNullable(o);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (id ? 1231 : 1237);
		result = prime * result + ((objectDefinition == null) ? 0 : objectDefinition.hashCode());
		result = prime * result + order;
		result = prime * result + ((parameterName == null) ? 0 : parameterName.hashCode());
		result = prime * result + ((parameterType == null) ? 0 : parameterType.hashCode());
		result = prime * result + ((sqlName == null) ? 0 : sqlName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SqlParameterDefinition other = (SqlParameterDefinition) obj;
		if (id != other.id)
			return false;
		if (objectDefinition == null) {
			if (other.objectDefinition != null)
				return false;
		}
		else if (!objectDefinition.equals(other.objectDefinition))
			return false;
		if (order != other.order)
			return false;
		if (parameterName == null) {
			if (other.parameterName != null)
				return false;
		}
		else if (!parameterName.equals(other.parameterName))
			return false;
		if (parameterType == null) {
			if (other.parameterType != null)
				return false;
		}
		else if (!parameterType.equals(other.parameterType))
			return false;
		if (sqlName == null) {
			if (other.sqlName != null)
				return false;
		}
		else if (!sqlName.equals(other.sqlName))
			return false;
		return true;
	}
	
}