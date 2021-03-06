package controllers

import conf.{Keys, PropertiesProviderComponent}
import models.ApplicationPropertyModels.MainTemplateProperties
import services.{AppProperty, ApplicationPropertiesServiceComponent}
import play.api.mvc.RequestHeader

trait PropertiesProvider {
  this: ApplicationPropertiesServiceComponent with PropertiesProviderComponent =>

  implicit def mainPageProperties(implicit request: RequestHeader): models.ApplicationPropertyModels.MainTemplateProperties = {
    val instanceName = propertiesService.getInstanceName
    val topBannerUrl = propertiesService.getBannerUrl(AppProperty.TopBannerUrl)
    val bottomBannerUrl = propertiesService.getBannerUrl(AppProperty.BottomBannerUrl)
    MainTemplateProperties(instanceName,
      if(topBannerUrl == null) None else topBannerUrl,
      if(bottomBannerUrl == null) None else bottomBannerUrl
    )
  }

}
