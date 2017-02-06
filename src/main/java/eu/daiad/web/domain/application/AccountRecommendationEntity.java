package eu.daiad.web.domain.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import com.fasterxml.jackson.core.JsonProcessingException;

import eu.daiad.web.model.message.Recommendation.ParameterizedTemplate;

@Entity(name = "account_recommendation")
@Table(schema = "public", name = "account_recommendation")
public class AccountRecommendationEntity
{
    @Id()
	@Column(name = "id")
	@SequenceGenerator(
	    sequenceName = "account_recommendation_id_seq",
	    name = "account_recommendation_id_seq",
	    allocationSize = 1,
	    initialValue = 1)
	@GeneratedValue(generator = "account_recommendation_id_seq", strategy = GenerationType.SEQUENCE)
	private int id;

	@ManyToOne()
	@JoinColumn(name = "account_id", nullable = false)
	@NotNull
	private AccountEntity account;

	@OneToOne(
	    mappedBy = "recommendation",
	    cascade = CascadeType.ALL,
	    fetch = FetchType.LAZY,
	    orphanRemoval = true
	)
	@JoinColumn(name = "account_recommendation_id")
	private AccountRecommendationParametersEntity parameters;

	@ManyToOne()
	@JoinColumn(name = "recommendation_template", nullable = false)
	@NotNull
	private RecommendationTemplateEntity recommendationTemplate;

	@Column(name = "created_on")
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	private DateTime createdOn;

	@Column(name = "acknowledged_on")
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	private DateTime acknowledgedOn;

	@Column(name = "receive_acknowledged_on")
	@Type(type = "org.jadira.usertype.dateandtime.joda.PersistentDateTime")
	private DateTime receiveAcknowledgedOn;

	public AccountRecommendationEntity() {}

	public AccountRecommendationEntity(
        AccountEntity account, RecommendationTemplateEntity templateEntity, ParameterizedTemplate parameterizedTemplate)
        throws IllegalArgumentException
    {
	    this.account = account;
        this.recommendationTemplate = templateEntity;
	    
        if (parameterizedTemplate != null) {
            try {
                parameters = new AccountRecommendationParametersEntity(this, parameterizedTemplate);
            } catch (JsonProcessingException ex) {
                throw new IllegalArgumentException("Failed to create parameters entity", ex);
            }
        }
    }
	
	public AccountRecommendationEntity(AccountEntity account, RecommendationTemplateEntity template)
	{
	    this(account, template, (ParameterizedTemplate) null);
	}

    public AccountEntity getAccount()
    {
		return account;
	}

	public void setAccount(AccountEntity account)
	{
		this.account = account;
	}

	public RecommendationTemplateEntity getTemplate()
	{
		return recommendationTemplate;
	}

	public DateTime getCreatedOn()
	{
		return createdOn;
	}

	public void setCreatedOn(DateTime createdOn) 
	{
		this.createdOn = createdOn;
	}

	public DateTime getAcknowledgedOn()
	{
		return acknowledgedOn;
	}

	public void setAcknowledgedOn(DateTime acknowledgedOn)
	{
		this.acknowledgedOn = acknowledgedOn;
	}

	public int getId() 
	{
		return id;
	}

	public AccountRecommendationParametersEntity getParameters()
    {
        return parameters;
    }

	public DateTime getReceiveAcknowledgedOn()
	{
		return receiveAcknowledgedOn;
	}

	public void setReceiveAcknowledgedOn(DateTime receiveAcknowledgedOn)
	{
		this.receiveAcknowledgedOn = receiveAcknowledgedOn;
	}
}